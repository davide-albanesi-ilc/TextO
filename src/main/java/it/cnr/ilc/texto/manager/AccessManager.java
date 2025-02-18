package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Group;
import it.cnr.ilc.texto.domain.GroupUser;
import it.cnr.ilc.texto.domain.Role;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.domain.Userable;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Closeable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class AccessManager extends Manager implements Closeable {

    @Autowired
    private DatabaseManager databaseManager;
    @Autowired
    private DomainManager domainManager;

    private AccessImplementation implementation;

    private AccessManager() {
    }

    @PostConstruct
    private void initImplementation() throws Exception {
        Class clazz = Class.forName(environment.getProperty("access.implementation-class"));
        implementation = (AccessImplementation) clazz.getConstructor().newInstance();
        implementation.init(environment, databaseManager, domainManager);
        implementation.init();
    }

    @Override
    public void close() {
        implementation.getSessions().stream().forEach(s -> s.getTimer().cancel());
    }

    public void startRequest(HttpServletRequest request) throws Exception {
        implementation.startRequest(request);
    }

    public User getUser() {
        return implementation.getUser();
    }

    public void endRequest() throws Exception {
        implementation.endRequest();
    }

    public String startSession(User user) throws Exception {
        return implementation.startSession(user);
    }

    public Session getSession() {
        return implementation.getSession();
    }

    public void endSession() throws Exception {
        implementation.endSession();
    }

    public Collection<Session> getSessions() {
        return implementation.getSessions();
    }

    public boolean contains(User user) {
        return implementation.getSessions().stream()
                .filter(s -> s.user.equals(user))
                .findAny().isPresent();
    }

    public static abstract class AccessImplementation {

        protected Environment environment;
        protected DatabaseManager databaseManager;
        protected DomainManager domainManager;

        private void init(Environment environment, DatabaseManager databaseManager, DomainManager domainManager) {
            this.environment = environment;
            this.databaseManager = databaseManager;
            this.domainManager = domainManager;
        }

        protected void init() throws Exception {
        }

        protected abstract void startRequest(HttpServletRequest request) throws Exception;

        protected abstract User getUser();

        protected abstract void endRequest() throws Exception;

        protected abstract String startSession(User user) throws Exception;

        protected abstract Session getSession();

        protected abstract void endSession() throws Exception;

        protected abstract Collection<Session> getSessions();

    }

    public static class Session {

        private User user;
        private String token;
        private LocalDateTime firstActionTime;
        private LocalDateTime lastActionTime;
        private LocalDateTime expiredTime;
        private final Map<String, Object> cache = new HashMap<>();

        @JsonIgnore
        private Timer timer;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public String getToken() {
            return token == null ? "void" : token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public LocalDateTime getFirstActionTime() {
            return firstActionTime;
        }

        public void setFirstActionTime(LocalDateTime firstActionTime) {
            this.firstActionTime = firstActionTime;
        }

        public LocalDateTime getLastActionTime() {
            return lastActionTime;
        }

        public void setLastActionTime(LocalDateTime lastActionTime) {
            this.lastActionTime = lastActionTime;
        }

        public LocalDateTime getExpiredTime() {
            return expiredTime;
        }

        public void setExpiredTime(LocalDateTime expiredTime) {
            this.expiredTime = expiredTime;
        }

        public Map<String, Object> getCache() {
            return cache;
        }

        public Timer getTimer() {
            return timer;
        }

        public void setTimer(Timer timer) {
            this.timer = timer;
        }

    }

    private final Map<String, Level> accesses = new HashMap<>();

    @PostConstruct
    private void initAccesses() throws SQLException, ReflectiveOperationException {
        try {
            String key;
            String sql = "select a.topic, a.action, r.id role, a.level from _access a join " + quote(Role.class) + " r on a.role = r.name";
            List<Map<String, Object>> list = databaseManager.query(sql);
            for (Map<String, Object> record : list) {
                key = record.get("topic") + " " + record.get("action") + " " + record.get("role");
                accesses.put(key, Level.valueOf(record.get("level").toString()));
            }
        } finally {
            databaseManager.releaseCommitConnection();
        }
    }

    public static class System {
    }

    public static enum Level {
        NONE,
        MINE,
        SHARE,
        FULL
    }

    private Level getLevel(String topic, Action action, User user) {
        try {
            return accesses.get(topic + " " + action + " " + user.getRole().getId());
        } catch (NullPointerException e) {
            return Level.NONE;
        }
    }

    public void checkAccess(Class clazz, Action action) throws ForbiddenException {
        if (!hasAccess(clazz, action)) {
            throw new ForbiddenException();
        }
    }

    public boolean hasAccess(Class clazz, Action action) {
        User user = getUser();
        Level level = getLevel(clazz.getSimpleName(), action, user);
        if (level == null || !level.equals(Level.FULL)) {
            return false;
        }
        return true;
    }

    public void checkAccess(Entity entity, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException {
        if (!hasAccess(entity, action)) {
            throw new ForbiddenException();
        }
    }

    public boolean hasAccess(Entity entity, Action action) throws ReflectiveOperationException, SQLException {
        User user = getUser();
        Level level = getLevel(entity.getClass().getSimpleName(), action, user);
        if (level == null || level.equals(Level.NONE)) {
            return false;
        } else if (level.equals(Level.MINE)) {
            if (!checkMine(entity, user)) {
                return false;
            }
        } else if (level.equals(Level.SHARE)) {
            if (!checkMine(entity, user) && !checkShare(entity, user, action)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkMine(Entity entity, User user) {
        if (entity instanceof Userable userable && userable.getUser() != null) {
            return userable.getUser().getId().equals(user.getId());
        } else {
            return false;
        }
    }

    private boolean checkShare(Entity entity, User user, Action action) throws ReflectiveOperationException, SQLException {
        String name = entity.getClass().getSimpleName();
        String field = name.substring(0, 1).toLowerCase().concat(name.substring(1)).concat("_id");
        String table = name.concat(User.class.getSimpleName());
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(table))
                .append(" where ").append(field).append(" = ").append(entity.getId())
                .append(" and user_id = ").append(user.getId())
                .append(" and action = '").append(action.name()).append("'");
        if (databaseManager.queryFirst(sql.toString()) != null) {
            return true;
        } else {
            table = name.concat(Group.class.getSimpleName());
            sql = new StringBuilder();
            sql.append("select * from ").append(quote(table)).append(" e ")
                    .append("join ").append(quote(GroupUser.class)).append(" u ")
                    .append("on e.group_id = u.group_id ")
                    .append("where e.").append(quote(field)).append(" = ").append(entity.getId())
                    .append(" and u.user_id = ").append(user.getId())
                    .append(" and e.action = '").append(action.name()).append("'");
            return databaseManager.queryFirst(sql.toString()) != null;
        }
    }

}
