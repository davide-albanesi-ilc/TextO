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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class SearchManager extends Manager {

    @Autowired
    private DatabaseManager databaseManager;
    @Lazy
    @Autowired
    private ResourceManager resourceManager;

    private List<Map<String, Object>> kwic(Resource resource, String query, Integer width) throws SQLException, ManagerException {
        width = width == null ? environment.getProperty("search.default-width", Integer.class, 10) : width;
        StringBuilder builder = new StringBuilder();
        builder.append("select t.*, l.start \"left\", a.token, r.end \"right\"\n")
                .append("from _analysis a\n")
                .append("join _token t on t.resource_id = a.resource_id and t.number = a.number\n")
                .append("join _token l on l.resource_id = t.resource_id and l.number = greatest(t.number-").append(width).append(", 0)\n")
                .append("join _token r on r.resource_id = t.resource_id and r.number = least(t.number+").append(width).append(", (select max(number) from _token where resource_id = ").append(resource.getId()).append("))\n")
                .append("where a.resource_id = ").append(resource.getId()).append(" and (").append(query).append(")");
        List<Map<String, Object>> list = databaseManager.query(builder.toString());
        String text = list.isEmpty() ? "" : resourceManager.getText(resource);
        return list.stream()
                .peek(r -> ((LinkedHashMap) r).putFirst("row", Map.of("id", r.remove("row_id"))))
                .peek(r -> ((LinkedHashMap) r).putFirst("resource", Map.of("id", r.remove("resource_id"))))
                .peek(r -> r.put("left", text.substring(((Number) r.get("left")).intValue(), ((Number) r.get("start")).intValue())))
                .peek(r -> r.put("right", text.substring(((Number) r.get("end")).intValue(), ((Number) r.get("right")).intValue())))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> kwic(Set<Resource> resources, String query, Integer width) throws SQLException, ManagerException {
        if (query == null || query.isEmpty()) {
            throw new ManagerException("null or empty query");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Resource resource : resources) {
            list.addAll(kwic(resource, query, width));
        }
        return list;
    }

}
