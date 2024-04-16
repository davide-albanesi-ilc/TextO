package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class SearchManager extends Manager {

    @Autowired
    private DatabaseManager databaseManager;

    public StringBuilder sqlKwic(Resource resource, String query, Integer width) throws SQLException {
        width = width == null ? environment.getProperty("search.default-width", Integer.class, 10) : width;
        StringBuilder builder = new StringBuilder();
        builder.append("select t.*, substring(x.text, l.start+1, t.start-l.start) \"left\", a.token, substring(x.text, t.end+1, r.end-t.end) \"right\"\n"
                + "from _analysis a\n"
                + "join _token t on t.resource_id = a.resource_id and t.number = a.number\n"
                + "join _token l on l.resource_id = t.resource_id and l.number = greatest(t.number-").append(width).append(", 0)\n"
                + "join _token r on r.resource_id = t.resource_id and r.number = least(t.number+").append(width).append(", (select max(number) from _token where resource_id = ").append(resource.getId()).append("))\n"
                + "join _text x on x.resource_id = t.resource_id\n"
                + "where a.resource_id = ").append(resource.getId()).append(" and (").append(query).append(")");
        return builder;
    }

    public List<Map<String, Object>> kwic(Resource resource, String query, Integer width) throws SQLException, ManagerException {
        if (query == null || query.isEmpty()) {
            throw new ManagerException("null or empty query");
        }
        StringBuilder builder = new StringBuilder();
        builder.append(sqlKwic(resource, query, width));
        return databaseManager.query(builder.toString()).stream()
                .peek(r -> ((LinkedHashMap) r).putFirst("row", Map.of("id", r.remove("row_id"))))
                .peek(r -> ((LinkedHashMap) r).putFirst("resource", Map.of("id", r.remove("resource_id"))))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> kwic(Set<Resource> resources, String query, Integer width) throws SQLException, ManagerException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Resource resource : resources) {
            list.addAll(kwic(resource, query, width));
        }
        return list;
    }

}
