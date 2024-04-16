package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.domain.SectionType;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.ResourceManager.checkOffset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class UtilManager extends Manager {

    @Autowired
    private ResourceManager resourceManager;
    @Autowired
    private SearchManager searchManager;
    @Autowired
    private DatabaseManager databaseManager;

    public Map<String, Object> getRows(Resource resource, Offset offset) throws SQLException, ManagerException {
        int length = checkOffset(offset, resourceManager.getRowCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select r.id, s.index, r.number absolute, r.relative, r.start, r.end, substring(t.text, r.start+1, r.end-r.start) text ")
                .append("from ").append(quote(Row.class)).append(" r ")
                .append("join _text t on r.resource_id = t.resource_id ")
                .append("left join ").append(quote(Section.class)).append(" s on s.id = r.section_id and s.status = 1 ")
                .append("where r.status = 1 and r.resource_id = ").append(resource.getId())
                .append(" and r.number >= ").append(offset.start).append(" and r.number < ").append(offset.end).append(" ")
                .append("order by r.number");
        List<Map<String, Object>> rows = databaseManager.query(sql.toString());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", length);
        map.put("start", offset.start);
        map.put("end", offset.end);
        map.put("data", rows);
        return map;
    }

    public List<Map<String, Object>> getSections(Resource resource, Section parent) throws SQLException, ReflectiveOperationException {
        String sql = "select s.id, t.id type_id, t.name type_name, s.start, s.end, s.title, s.index,\n"
                + "(select number from " + quote(Row.class) + " where resource_id = s.resource_id and status = 1 and start = s.start) row_start,\n"
                + "(select number from " + quote(Row.class) + " where resource_id = s.resource_id and status = 1 and end = s.end) row_end\n"
                + "from " + quote(Section.class) + " s join " + quote(SectionType.class) + " t on s.type_id = t.id and t.status = 1\n"
                + "where s.resource_id = " + resource.getId() + " and s.parent_id" + (parent == null ? " is null" : (" = " + parent.getId()))
                + " and s.status = 1\n"
                + "order by s.start";
        List<Map<String, Object>> sections = databaseManager.query(sql);
        sections.forEach(s -> {
            s.put("type", Map.of("id", s.remove("type_id"), "name", s.remove("type_name")));
        });
        return sections;
    }

    public List<Map<String, Object>> getSections(Resource resource) throws SQLException, ReflectiveOperationException {
        List<Map<String, Object>> sections = new ArrayList<>();
        String sql = "select id, name from " + quote(SectionType.class) + " where status = 1 and resource_id = " + resource.getId();
        Map<Long, Map<String, Object>> types = databaseManager.query(sql).stream()
                .collect(Collectors.toMap(t -> ((Number) t.get("id")).longValue(), t -> t));
        sql = "select s.id, s.parent_id, s.type_id, s.start, s.end, s.title, s.index, min(r.number) row_start, max(r.number) row_end\n"
                + "from " + quote(Section.class) + " s left join " + quote(Row.class) + " r on s.id = r.section_id and r.status = 1\n"
                + "where s.status = 1 and s.resource_id = " + resource.getId() + "\n"
                + "group by s.id, s.parent_id, s.type_id, s.start, s.end, s.title, s.index\n"
                + "order by s.start";
        List<Map<String, Object>> list = databaseManager.query(sql);
        Map<Long, Map<String, Object>> map = list.stream()
                .collect(Collectors.toMap(a -> ((Number) a.get("id")).longValue(), a -> a));
        for (Map<String, Object> current : list) {
            current.put("type", types.get(((Number) current.remove("type_id")).longValue()));
            Number parentId = (Number) current.remove("parent_id");
            if (parentId != null) {
                Map<String, Object> parent = map.get(parentId.longValue());
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                if (children == null) {
                    children = new ArrayList<>();
                    parent.put("children", children);
                }
                children.add(current);
            } else {
                sections.add(current);
            }
        }
        for (Map<String, Object> section : sections) {
            fillIndex(section);
        }
        return sections;
    }

    private void fillIndex(Map<String, Object> section) {
        Integer index;
        List<Map<String, Object>> children = (List<Map<String, Object>>) section.get("children");
        if (children != null) {
            for (Map<String, Object> child : children) {
                fillIndex(child);
            }
            index = (Integer) children.get(0).get("row_start");
            section.put("row_start", index);
            index = (Integer) children.get(children.size() - 1).get("row_end");
            section.put("row_end", index);
        }
    }

    public List<Map<String, Object>> getAnnotations(Resource resource, Offset offset) throws SQLException, ReflectiveOperationException, ManagerException {
        offset = resourceManager.getAbsoluteOffset(resource, offset);
        checkOffset(offset, resourceManager.getCharacterCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select a.id, a.layer_id, a.start, a.end, a.user_id, af.id \"af_id\", af.feature_id, af.value ")
                .append("from ").append(quote(Annotation.class)).append(" a ")
                .append("join ").append(quote(AnnotationFeature.class)).append(" af on af.annotation_id = a.id and af.status = 1 and a.status = 1 ")
                .append("where a.resource_id = ").append(resource.getId()).append(" and a.start >= ").append(offset.start).append(" and a.end <= ").append(offset.end)
                .append(" order by a.start");
        List<Map<String, Object>> annotations = databaseManager.query(sql.toString());
        List<Map<String, Object>> annotationFeatures;
        Map<String, Object> annotationFeature;
        Map<String, Object> previous = null;
        List<Map<String, Object>> returns = new ArrayList<>(annotations.size());
        for (Map<String, Object> annotation : annotations) {
            if (previous == null || !previous.get("id").equals(annotation.get("id"))) {
                annotation.put("layer", Map.of("id", annotation.remove("layer_id")));
                annotation.put("user", Map.of("id", annotation.remove("user_id")));
                annotationFeatures = new ArrayList<>();
                annotationFeature = new LinkedHashMap<>();
                annotationFeature.put("id", annotation.remove("af_id"));
                annotationFeature.put("feature", Map.of("id", annotation.remove("feature_id")));
                annotationFeature.put("value", annotation.remove("value"));
                annotationFeatures.add(annotationFeature);
                annotation.put("features", annotationFeatures);
                returns.add(annotation);
                previous = annotation;
            } else {
                annotationFeatures = (List<Map<String, Object>>) previous.get("features");
                annotationFeature = new LinkedHashMap<>();
                annotationFeature.put("id", annotation.remove("af_id"));
                annotationFeature.put("feature", Map.of("id", annotation.remove("feature_id")));
                annotationFeature.put("value", annotation.remove("value"));
                annotationFeatures.add(annotationFeature);
            }
        }
        return returns;
    }

    public List<Map<String, Object>> kwic(List<Resource> resources, String query, String filter, Integer width) throws SQLException, ManagerException {
        if (resources == null || resources.isEmpty()) {
            throw new ManagerException("null or empty resources");
        }
        if (query == null || query.isEmpty()) {
            throw new ManagerException("null or empty query");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("select * from (select row_number() over (order by t.resource_id, t.row_number) id, t.* from (\n");
        for (Resource resource : resources) {
            builder.append("select * from (")
                    .append("select k.resource_id, r.name resource_name, w.section_id, s.index section_index, k.row_id, w.number \"row_number\", k.left left_context, k.token, k.right right_context\n")
                    .append("from (").append(searchManager.sqlKwic(resource, query, width)).append(") k\n")
                    .append("join ").append(quote(Resource.class)).append(" r on r.id = k.resource_id and r.status = 1\n")
                    .append("join ").append(quote(Row.class)).append(" w on w.id = k.row_id and w.status = 1\n")
                    .append("left join ").append(quote(Section.class)).append(" s on s.id = w.section_id and s.status = 1\n")
                    .append("order by k.number) s\n")
                    .append("union\n");
        }
        builder.setLength(builder.length() - 6);
        builder.append(") t ) t ");
        if (filter != null && !filter.isEmpty()) {
            builder.append(" where ").append(filter);
        }
        return databaseManager.query(builder.toString());
    }

}
