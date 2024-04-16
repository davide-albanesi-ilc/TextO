package it.cnr.ilc.texto.manager;

import static it.cnr.ilc.texto.manager.FolderManager.MAX_PATH_DEPTH;
import static it.cnr.ilc.texto.manager.DomainManager.sqlValue;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.uploader.PlainTextUploader;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class ResourceManager extends EntityManager<Resource> {

    @Lazy
    @Autowired
    private FolderManager folderManager;
    @Autowired
    private MonitorManager monitorManager;

    private final Map<String, Uploader> uploaders = new HashMap<>();

    @PostConstruct
    private void initUploader() throws Exception {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(PlainTextUploader.class.getPackageName()));
        Collection<Class<? extends Uploader>> classes = reflections.getSubTypesOf(Uploader.class);
        for (Class<? extends Uploader> clazz : classes) {
            Uploader uploader = clazz.getConstructor().newInstance();
            uploader.init(environment, databaseManager, domainManager, monitorManager);
            uploader.init();
            uploaders.put(uploader.name(), uploader);
        }
    }

    @Override
    protected Class<Resource> entityClass() {
        return Resource.class;
    }

    @Override
    public String getLog(Resource resource) throws SQLException {
        StringBuilder path = new StringBuilder();
        if (resource.getParent() != null) {
            path.append(folderManager.getPath(resource.getParent())).append("/");
        }
        path.append(resource.getName());
        return path.toString();
    }

    @Check
    public void exists(Resource previous, Resource resource) throws SQLException, ReflectiveOperationException, ManagerException {
        if (folderManager.exists(resource.getParent(), resource.getName())) {
            throw new ManagerException("name exsists");
        }
    }

    public Set<String> getUploaders() {
        return uploaders.keySet();
    }

    public String getPath(Resource resource) throws SQLException {
        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        select.append("select concat('/', concat_ws('/', r0.name, f").append(MAX_PATH_DEPTH - 1).append(".name");
        from.append("from ").append(quote(Resource.class)).append(" r0 join ").append(quote(Folder.class)).append(" f0 ")
                .append("on f0.id = r0.parent_id and r0.status = 1 and f0.status = 1\n");
        for (int i = 1; i < MAX_PATH_DEPTH; i++) {
            select.append(", f").append(MAX_PATH_DEPTH - 1 - i).append(".name");
            from.append("left join ").append(quote(Folder.class)).append(" f").append(i)
                    .append(" on f").append(i).append(".id = f").append(i - 1).append(".parent_id")
                    .append(" and f").append(i).append(".status = 1 and f").append(i - 1).append(".status = 1\n");
        }
        select.append(")) path \n").append(from).append("where r0.id = ").append(resource.getId());
        return databaseManager.queryFirst(select.toString(), String.class);
    }

    public int getCharacterCount(Resource resource) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("select length(text) from _text where resource_id = ").append(resource.getId());
        Number number = databaseManager.queryFirst(sql.toString(), Number.class);
        return number == null ? 0 : number.intValue();
    }

    public int getRowCount(Resource resource) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ").append(quote(Row.class)).append(" where resource_id = ").append(resource.getId());
        Number number = databaseManager.queryFirst(sql.toString(), Number.class);
        return number == null ? 0 : number.intValue();
    }

    public boolean exists(Folder parent, String name) throws SQLException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(id) from ").append(quote(Resource.class))
                .append(" where status = 1")
                .append(" and name = ").append(sqlValue(name))
                .append(" and parent_id = ").append(parent.getId());
        return databaseManager.queryFirst(sql.toString(), Number.class).intValue() > 0;
    }

    public void upload(Resource resource, String source, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        String name = parameters.getOrDefault("uploader", environment.getProperty("resource.default-uploader", "plain-text"));
        Uploader uploader = uploaders.get(name);
        if (uploader == null) {
            throw new ManagerException("invalid uploader " + name);
        }
        source = uploader.upload(resource, source, parameters);
        databaseManager.update("delete from _text where resource_id = " + resource.getId());
        String sql = "insert into _text (resource_id, text) values (" + resource.getId() + ", ?)";
        try (PreparedStatement statement = databaseManager.getConnection().prepareStatement(sql)) {
            statement.setBytes(1, source.getBytes());
            LoggerFactory.getLogger(DatabaseManager.class).debug(sql);
            statement.executeUpdate();
        }
    }

    public InputStream download(Resource resource) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("select text from _text where resource_id = ").append(resource.getId());
        return databaseManager.getInputStream(sql.toString());
    }

    public Offset getAbsoluteOffset(Resource resource, Offset offset) throws SQLException, ManagerException {
        checkOffset(offset, getRowCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select min(start) start, max(end) end from ").append(quote(Row.class))
                .append(" where status = 1 and resource_id = ").append(resource.getId())
                .append(" and number >= ").append(offset.start).append(" and number <= ").append(offset.end);
        Map<String, Object> record = databaseManager.queryFirst(sql.toString());
        Offset abbsolute = new Offset();
        abbsolute.start = ((Number) record.get("start")).intValue();
        abbsolute.end = ((Number) record.get("end")).intValue();
        return abbsolute;
    }

    public static int checkOffset(Offset offset, int length) throws SQLException, ManagerException {
        if (offset.start == null) {
            offset.start = 0;
        }
        if (offset.end == null) {
            offset.end = length;
        }
        if (offset.start < 0 || offset.start > length || offset.end < 0 || offset.start > offset.end) {
            throw new ManagerException("index out of bounds");
        }
        return length;
    }

    public String getText(Resource resource, Offset offset) throws SQLException, ManagerException {
        checkOffset(offset, getCharacterCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select substr(text, ")
                .append(offset.start + 1).append(", ").append(offset.end - offset.start)
                .append(") from _text where resource_id = ").append(resource.getId());
        return databaseManager.queryFirst(sql.toString(), String.class);
    }

    public static abstract class Uploader {

        protected Environment environment;
        protected DatabaseManager databaseManager;
        protected DomainManager domainManager;
        protected MonitorManager monitorManager;

        private void init(Environment environment, DatabaseManager databaseManager, DomainManager domainManager, MonitorManager monitorManager) {
            this.environment = environment;
            this.databaseManager = databaseManager;
            this.domainManager = domainManager;
            this.monitorManager = monitorManager;
        }

        protected void init() throws SQLException, ReflectiveOperationException, ManagerException {
        }

        protected abstract String name();

        protected abstract String upload(Resource resource, String source, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException;

    }
}
