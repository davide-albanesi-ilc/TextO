package it.cnr.ilc.texto.manager;

import static it.cnr.ilc.texto.manager.FolderManager.MAX_PATH_DEPTH;
import static it.cnr.ilc.texto.manager.DomainManager.sqlValue;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.annotation.Check;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.BreakIterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
public class ResourceManager extends EntityManager<Resource> {

    @Lazy
    @Autowired
    private FolderManager folderManager;

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

    public String getPath(Resource resource) throws SQLException {
        StringBuilder select = new StringBuilder();
        StringBuilder from = new StringBuilder();
        select.append("select concat('/', concat_ws('/', r0.name, f").append(MAX_PATH_DEPTH - 1).append(".name");
        from.append("from Resource r0 join Folder f0 on f0.id = r0.parent_id and r0.status = 1 and f0.status = 1\n");
        for (int i = 1; i < MAX_PATH_DEPTH; i++) {
            select.append(", f").append(MAX_PATH_DEPTH - 1 - i).append(".name");
            from.append("left join Folder f").append(i)
                    .append(" on f").append(i).append(".id = f").append(i - 1).append(".parent_id")
                    .append(" and f").append(i).append(".status = 1 and f").append(i - 1).append(".status = 1\n");
        }
        select.append(")) path \n").append(from).append("where r0.id = ").append(resource.getId());
        return databaseManager.queryFirst(select.toString(), String.class);
    }

    public int getCharacterCount(Resource resource) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("select length(text) from _text where resource_id = ").append(resource.getId());
        return databaseManager.queryFirst(sql.toString(), Number.class).intValue();
    }

    public int getRowCount(Resource resource) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from _rows where resource_id = ").append(resource.getId());
        return databaseManager.queryFirst(sql.toString(), Number.class).intValue();
    }

    public boolean exists(Folder parent, String name) throws SQLException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select count(id) from ").append(quote(Resource.class))
                .append(" where status = 1")
                .append(" and name = ").append(sqlValue(name))
                .append(" and parent_id = ").append(parent.getId());
        return databaseManager.queryFirst(sql.toString(), Number.class).intValue() > 0;
    }

    public void upload(Resource resource, InputStream input) throws SQLException, ManagerException {
        String sql = "delete from _text where resource_id = " + resource.getId();
        databaseManager.update(sql);
        sql = "insert into _text (resource_id, text) values (?, ?)";
        try (PreparedStatement statement = databaseManager.getConnection().prepareStatement(sql)) {
            statement.setLong(1, resource.getId());
            statement.setCharacterStream(2, new InputStreamReader(input));
            statement.executeUpdate();
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
        insertRows(resource.getId());
    }

    private void insertRows(Long resourceId) throws SQLException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select text from _text where resource_id = ").append(resourceId);
        String text = databaseManager.queryFirst(sql.toString(), String.class);
        if (text.isBlank()) {
            throw new ManagerException("empty text not allowed");
        }
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ITALIAN);
        iterator.setText(text);
        int start = iterator.first();
        int end = iterator.next();
        int id = 0;
        while (end != BreakIterator.DONE) {
            sql = new StringBuilder();
            sql.append("insert into _rows values (")
                    .append(resourceId).append(", ")
                    .append(id).append(",")
                    .append(start).append(", ")
                    .append(end).append(", ")
                    .append("'").append(text.substring(start, end).replaceAll("'", "''")).append("')");
            databaseManager.update(sql.toString());
            id++;
            start = end;
            end = iterator.next();
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
        sql.append("select min(start) start, max(end) end from _rows ")
                .append("where resource_id = ").append(resource.getId())
                .append(" and id >= ").append(offset.start).append(" and id < ").append(offset.end);
        Map<String, Object> record = databaseManager.queryFirst(sql.toString());
        Offset abbsolute = new Offset();
        abbsolute.start = ((Number) record.get("start")).intValue();
        abbsolute.end = ((Number) record.get("end")).intValue();
        return abbsolute;
    }

    public int checkOffset(Offset offset, int length) throws SQLException, ManagerException {
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

    public Map<String, Object> getRows(Resource resource, Offset offset) throws SQLException, ManagerException {
        int length = checkOffset(offset, getRowCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select id, start, end, text from _rows where resource_id = ").append(resource.getId())
                .append(" and id >= ").append(offset.start).append(" and id < ").append(offset.end);
        List<Map<String, Object>> strings = databaseManager.query(sql.toString());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", length);
        map.put("start", offset.start);
        map.put("end", offset.end);
        if (!strings.isEmpty()) {
            map.put("offset", strings.get(0).get("start"));
        }
        map.put("data", strings.stream().map(r -> (String) r.get("text")).collect(Collectors.toList()));
        return map;
    }

}
