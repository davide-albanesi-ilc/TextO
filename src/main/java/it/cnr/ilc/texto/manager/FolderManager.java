package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Listable;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Status;
import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.DomainManager.quoteHistory;
import static it.cnr.ilc.texto.manager.DomainManager.sqlValue;
import it.cnr.ilc.texto.manager.annotation.Check;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class FolderManager extends EntityManager<Folder> {

    public static final int MAX_PATH_DEPTH = 16;

    @Lazy
    @Autowired
    private ResourceManager resourceManager;

    @Override
    protected Class<Folder> entityClass() {
        return Folder.class;
    }

    @Override
    public String getLog(Folder folder) throws SQLException {
        return getPath(folder);
    }

    @Check
    public void exists(Folder previous, Folder folder) throws SQLException, ReflectiveOperationException, ManagerException {
        if (folder.getParent() != null && resourceManager.exists(folder.getParent(), folder.getName())) {
            throw new ManagerException("name exsists");
        }
    }

    public String getPath(Folder folder) throws SQLException {
        if (folder == null) {
            return "/";
        }
        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        select.append("select concat('/', concat_ws('/', f").append(MAX_PATH_DEPTH - 1).append(".name");
        from.append("from ").append(quote(Folder.class)).append(" f0\n");
        for (int i = 1; i < MAX_PATH_DEPTH; i++) {
            select.append(", f").append(MAX_PATH_DEPTH - 1 - i).append(".name");
            from.append("left join ").append(quote(Folder.class)).append(" f").append(i)
                    .append(" on f").append(i).append(".id = f").append(i - 1).append(".parent_id\n");
        }
        select.append(")) path \n").append(from)
                .append("where f0.id = ").append(folder.getId());
        return databaseManager.queryFirst(select.toString(), String.class);
    }

    public boolean exists(Folder parent, String name) throws SQLException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(id) from ").append(quote(Folder.class))
                .append(" where name = ").append(sqlValue(name))
                .append(" and parent_id = ").append(parent.getId());
        return databaseManager.queryFirst(sql.toString(), Number.class).intValue() > 0;
    }

    public Folder getHome(User user) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Folder.class))
                .append(" where parent_id is null");
        if (!Boolean.TRUE.equals(environment.getProperty("folder.shared-home", Boolean.class))) {
            sql.append(" and user_id = ").append(user.getId());
        }
        return loadUnique(sql.toString());
    }

    public void createHome(User user) throws SQLException, ReflectiveOperationException, ManagerException {
        if (!Boolean.TRUE.equals(environment.getProperty("folder.shared-home", Boolean.class))) {
            Long id = domainManager.newId();
            StringBuilder sql = new StringBuilder();
            sql.append("insert into ").append(quote(Folder.class))
                    .append(" (id, status, time, name, user_id)")
                    .append(" values (").append(id).append(", 1, now(), '")
                    .append(user.getUsername()).append("', ").append(user.getId()).append(")");
            databaseManager.update(sql.toString());
        }
    }

    public void removeHome(User user) throws SQLException, ReflectiveOperationException, ManagerException {
        if (!Boolean.TRUE.equals(environment.getProperty("folder.shared-home", Boolean.class))) {
            Folder home = getHome(user);
            if (home == null) {
                throw new ManagerException("home not found");
            }
            StringBuilder sql = new StringBuilder();
            sql.append("insert into ").append(quoteHistory(Folder.class))
                    .append(" (id, status, time, name, description, user_id)")
                    .append(" select id, ").append(Status.HISTORY).append(", time, name, description, user_id ")
                    .append("from ").append(quote(Folder.class)).append(" where id = ").append(home.getId());
            databaseManager.update(sql.toString());
            sql.append("insert into ").append(quoteHistory(Folder.class))
                    .append(" (id, status, time, name, description, user_id)")
                    .append(" select id, ").append(Status.REMOVED).append(", now(), name, description, user_id ")
                    .append("from ").append(quote(Folder.class)).append(" where id = ").append(home.getId());
            databaseManager.update(sql.toString());
            sql = new StringBuilder();
            sql.append("delete from ").append(quote(Folder.class))
                    .append(" where id = ").append(home.getId());
            databaseManager.update(sql.toString());
        }
    }

    private Map<String, Object> toMap(Listable listable) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (listable instanceof Folder folder) {
            map.put("id", folder.getId());
            map.put("type", Folder.class.getSimpleName());
            map.put("parent", listable.getParent());
            map.put("name", listable.getName());
            map.put("description", folder.getDescription());
            map.put("user", folder.getUser());
        } else if (listable instanceof Resource resource) {
            map.put("id", resource.getId());
            map.put("type", Resource.class.getSimpleName());
            map.put("parent", listable.getParent());
            map.put("name", listable.getName());
            map.put("description", resource.getDescription());
            map.put("user", resource.getUser());
        }
        return map;
    }

    private List<Map<String, Object>> toMapList(List<Listable> listables) {
        return listables.stream().map(l -> toMap(l)).collect(Collectors.toList());
    }

    public List<Map<String, Object>> list(Folder folder) throws SQLException, ReflectiveOperationException, ManagerException {
        return toMapList(list(folder.getId()));
    }

    private List<Listable> list(Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Folder.class))
                .append(" where parent_id = ").append(id);
        List<Listable> list = new ArrayList<>(load(sql.toString()));
        sql = new StringBuilder();
        sql.append("select * from ").append(quote(Resource.class))
                .append(" where parent_id = ").append(id);
        list.addAll(domainManager.load(Resource.class, sql.toString()));
        Collections.sort(list, (l1, l2) -> l1.getName().compareTo(l2.getName()));
        return list;
    }

    public List<Map<String, Object>> tree(Folder folder) throws SQLException, ReflectiveOperationException, ManagerException {
        return tree(folder.getId());
    }

    private List<Map<String, Object>> tree(Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        List<Map<String, Object>> list = toMapList(list(id));
        for (Map<String, Object> map : list) {
            if (map.get("type").equals(Folder.class.getSimpleName())) {
                map.put("children", tree((Long) map.get("id")));
            }
        }
        return list;
    }

}
