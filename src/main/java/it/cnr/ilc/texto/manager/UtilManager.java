package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Analysis;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.domain.SectionType;
import it.cnr.ilc.texto.domain.Token;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.ResourceManager.checkOffset;
import it.cnr.ilc.texto.util.Pair;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private DatabaseManager databaseManager;

    public Map<String, Object> getRows(Resource resource, Offset offset) throws SQLException, ManagerException {
        int length = checkOffset(offset, resourceManager.getRowCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select r.id, s.index, r.number absolute, r.relative, r.start, r.end\n")
                .append("from ").append(quote(Row.class)).append(" r\n")
                .append("left join ").append(quote(Section.class)).append(" s on s.id = r.section_id\n")
                .append("where r.resource_id = ").append(resource.getId())
                .append(" and r.number >= ").append(offset.getStart()).append(" and r.number < ").append(offset.getEnd()).append("\n")
                .append("order by r.number");
        List<Map<String, Object>> rows = databaseManager.query(sql.toString());
        String text = rows.isEmpty() ? "" : resourceManager.getText(resource);
        rows = rows.stream()
                .peek(r -> r.put("text", text.substring(((Number) r.get("start")).intValue(), ((Number) r.get("end")).intValue())))
                .toList();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", length);
        map.put("start", offset.getStart());
        map.put("end", offset.getEnd());
        map.put("data", rows);
        return map;
    }

    public List<Map<String, Object>> getSections(Resource resource, Section parent) throws SQLException, ReflectiveOperationException {
        String sql = "select s.id, t.id type_id, t.name type_name, s.start, s.end, s.title, s.index,\n"
                + "(select number from " + quote(Row.class) + " where resource_id = s.resource_id and start = s.start) row_start,\n"
                + "(select number from " + quote(Row.class) + " where resource_id = s.resource_id and end = s.end) row_end\n"
                + "from " + quote(Section.class) + " s join " + quote(SectionType.class) + " t on s.type_id = t.id\n"
                + "where s.resource_id = " + resource.getId() + " and s.parent_id" + (parent == null ? " is null" : (" = " + parent.getId())) + "\n"
                + "order by s.start";
        List<Map<String, Object>> sections = databaseManager.query(sql);
        sections.forEach(s -> {
            s.put("type", Map.of("id", s.remove("type_id"), "name", s.remove("type_name")));
        });
        return sections;
    }

    public List<Map<String, Object>> getSections(Resource resource) throws SQLException, ReflectiveOperationException {
        List<Map<String, Object>> sections = new ArrayList<>();
        String sql = "select id, name from " + quote(SectionType.class) + " where resource_id = " + resource.getId();
        Map<Long, Map<String, Object>> types = databaseManager.query(sql).stream()
                .collect(Collectors.toMap(t -> ((Number) t.get("id")).longValue(), t -> t));
        sql = "select s.id, s.parent_id, s.type_id, s.start, s.end, s.title, s.index, min(r.number) row_start, max(r.number) row_end\n"
                + "from " + quote(Section.class) + " s left join " + quote(Row.class) + " r on s.id = r.section_id\n"
                + "where s.resource_id = " + resource.getId() + "\n"
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

    public List<Map<String, Object>> getAnnotations(Resource resource, List<Layer> layers, Offset offset) throws SQLException, ReflectiveOperationException, ManagerException {
        offset = resourceManager.getAbsoluteOffset(resource, offset);
        checkOffset(offset, resourceManager.getCharacterCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select a.id, a.layer_id, a.start, a.end, a.user_id, af.id \"af_id\", af.feature_id, af.value\n")
                .append("from ").append(quote(Annotation.class)).append(" a\n")
                .append("left join ").append(quote(AnnotationFeature.class)).append(" af on af.annotation_id = a.id\n")
                .append("where a.resource_id = ").append(resource.getId()).append("\n")
                .append(" and a.start >= ").append(offset.getStart()).append(" and a.end < ").append(offset.getEnd()).append("\n");
        if (layers != null && !layers.isEmpty()) {
            sql.append(" and a.layer_id in ").append(joiningIds(layers)).append("\n");
        }
        sql.append("order by a.start");
        List<Map<String, Object>> annotations = databaseManager.query(sql.toString());
        List<Map<String, Object>> annotationFeatures;
        Map<String, Object> annotationFeature;
        Map<String, Object> previous = null;
        Object annotationFeatureId;
        List<Map<String, Object>> returns = new ArrayList<>(annotations.size());
        for (Map<String, Object> annotation : annotations) {
            if (previous == null || !previous.get("id").equals(annotation.get("id"))) {
                annotation.put("layer", Map.of("id", annotation.remove("layer_id")));
                annotation.put("user", Map.of("id", annotation.remove("user_id")));
                annotationFeatures = new ArrayList<>();
                if ((annotationFeatureId = annotation.remove("af_id")) != null) {
                    annotationFeature = new LinkedHashMap<>();
                    annotationFeature.put("id", annotationFeatureId);
                    annotationFeature.put("feature", Map.of("id", annotation.remove("feature_id")));
                    annotationFeature.put("value", annotation.remove("value"));
                    annotationFeatures.add(annotationFeature);
                }
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

    public List<Map<String, Object>> kwic(List<Resource> resources, String query, Integer width, Layer layer, List<Pair<Feature, String[]>> features) throws SQLException, ManagerException {
        width = width == null ? environment.getProperty("search.default-width", Integer.class, 10) : width;
        if (resources == null || resources.isEmpty()) {
            throw new ManagerException("null or empty resources");
        }
        if (query == null || query.isEmpty()) {
            throw new ManagerException("null or empty query");
        }
        query = query.replaceAll("token", "value");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Resource resource : resources) {
            list.addAll(kwic(resource, query, width, layer, features));
        }
        return list;
    }

    private List<Map<String, Object>> kwic(Resource resource, String query, int width, Layer layer, List<Pair<Feature, String[]>> features) throws SQLException, ManagerException {
        StringBuilder builder = new StringBuilder();
        builder.append("select distinct\n")
                .append(" t.resource_id,\n")
                .append(" rs.name \"resource_name\",\n")
                .append(" sc.id section_id,\n")
                .append(" sc.index \"section_index\",\n")
                .append(" t.row_id,\n ")
                .append(" rw.number \"row_number\",\n")
                .append(" t.number,\n")
                .append(" t.start,\n")
                .append(" t.end,\n")
                .append(" l.start \"left_start\",\n")
                .append(" r.end \"right_end\",\n")
                .append(" a.value \"token\",\n");
        if (layer != null) {
            builder.append(" (select exists (select id from `Annotation` n where n.resource_id = a.resource_id and n.layer_id = ")
                    .append(layer.getId())
                    .append(" and n.start = t.start and t.end = n.end)) \"annotated\"\n");
        } else {
            builder.append(" null \"annotated\"\n");
        }
        builder.append("from ").append(quote(Analysis.class)).append(" a\n")
                .append("join ").append(quote(Token.class)).append(" t on t.id = a.token_id\n")
                .append("join ").append(quote(Token.class)).append(" l on l.resource_id = t.resource_id and l.number = greatest(t.number-").append(width).append(", 0)\n")
                .append("join ").append(quote(Token.class)).append(" r on r.resource_id = t.resource_id and r.number = least(t.number+").append(width).append(", (select max(number) from ").append(quote(Token.class)).append(" where resource_id = ").append(resource.getId()).append("))\n")
                .append("join ").append(quote(Row.class)).append(" rw on rw.id = t.row_id\n")
                .append("join ").append(quote(Section.class)).append(" sc on sc.id = rw.section_id\n")
                .append("join ").append(quote(Resource.class)).append(" rs on rs.id = a.resource_id\n")
                .append("where a.resource_id = ").append(resource.getId()).append(" and (").append(query).append(")\n");
        if (features != null) {
            for (Pair<Feature, String[]> feature : features) {
                builder.append("and exists (select n.id from ").append(quote(AnnotationFeature.class)).append(" f join ").append(quote(Annotation.class)).append(" n on n.id = f.annotation_id \n")
                        .append(" where n.resource_id = a.resource_id and f.feature_id = ").append(feature.getFirst().getId()).append(" and n.start = t.start and n.end = t.end\n")
                        .append(" and f.value in ").append(formatSqlIn(feature.getSecond())).append(")\n");
            }
        }
        builder.append("order by t.start");
        List<Map<String, Object>> list = databaseManager.query(builder.toString());
        String text = list.isEmpty() ? "" : resourceManager.getText(resource);
        return list.stream()
                .peek(r -> r.put("left", text.substring(((Number) r.get("left_start")).intValue(), ((Number) r.get("start")).intValue())))
                .peek(r -> r.put("right", text.substring(((Number) r.get("end")).intValue(), ((Number) r.get("right_end")).intValue())))
                .peek(r -> {
                    Number annotated = (Number) r.get("annotated");
                    if (annotated != null) {
                        r.put("annotated", annotated.intValue() != 0);
                    }
                })
                .collect(Collectors.toList());
    }

    private String formatSqlIn(String[] values) {
        return Stream.of(values)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(",", "(", ")"));
    }

    public List<Map<String, Object>> aic(List<Resource> resources, Long featureId, String value, Integer width) throws SQLException, ManagerException {
        width = width == null ? environment.getProperty("search.default-width", Integer.class, 10) : width;
        if (resources == null || resources.isEmpty()) {
            throw new ManagerException("null or empty resources");
        }
        if (featureId == null) {
            throw new ManagerException("feature id missing");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Resource resource : resources) {
            list.addAll(aic(resource, featureId, value, width));
        }
        return list;
    }

    private List<Map<String, Object>> aic(Resource resource, Long featureId, String value, int width) throws SQLException, ManagerException {
        StringBuilder builder = new StringBuilder();
        builder.append("select\n")
                .append(" rs.id \"resource_id\",\n")
                .append(" rs.name \"resource_name\",\n")
                .append(" sl.id \"section_left_id\",\n")
                .append(" sl.index \"section_left_index\",\n")
                .append(" sr.id \"section_right_id\",\n")
                .append(" sr.index \"section_right_index\",\n")
                .append(" rl.id \"row_left_id\",\n")
                .append(" rl.number \"row_left_number\",\n")
                .append(" rr.id \"row_right_id\",\n")
                .append(" rr.number \"row_right_number\",\n")
                .append(" a.start \"start\",\n")
                .append(" a.end \"end\",\n")
                .append(" cl.start \"left_start\",\n")
                .append(" cr.end \"right_end\"\n")
                .append("from ").append(quote(Annotation.class)).append(" a\n")
                .append("join ").append(quote(AnnotationFeature.class)).append(" af on af.annotation_id = a.id\n")
                .append("join ").append(quote(Token.class)).append(" tl on tl.resource_id = a.resource_id and tl.start = a.start\n")
                .append("join ").append(quote(Token.class)).append(" tr on tr.resource_id = a.resource_id and tr.end = a.end\n")
                .append("join ").append(quote(Token.class)).append(" cl on cl.resource_id = a.resource_id and cl.number = greatest(tl.number-").append(width).append(", 0)\n")
                .append("join ").append(quote(Token.class)).append(" cr on cr.resource_id = a.resource_id and cr.number = least(tr.number+").append(width).append(", (select max(number) from ").append(quote(Token.class)).append(" where resource_id = ").append(resource.getId()).append("))\n")
                .append("join ").append(quote(Row.class)).append(" rl on rl.id = tl.row_id\n")
                .append("join ").append(quote(Row.class)).append(" rr on rr.id = tr.row_id\n")
                .append("join ").append(quote(Section.class)).append(" sl on sl.id = rl.section_id\n")
                .append("join ").append(quote(Section.class)).append(" sr on sr.id = rr.section_id\n")
                .append("join ").append(quote(Resource.class)).append(" rs on rs.id = a.resource_id\n")
                .append("where a.resource_id = ").append(resource.getId()).append(" and af.feature_id = ").append(featureId).append(" and af.value = '").append(value).append("'\n")
                .append("order by tl.start");
        List<Map<String, Object>> list = databaseManager.query(builder.toString());
        String text = list.isEmpty() ? "" : resourceManager.getText(resource);
        return list.stream()
                .peek(r -> r.put("annotation", text.substring(((Number) r.get("start")).intValue(), ((Number) r.get("end")).intValue())))
                .peek(r -> r.put("left", text.substring(((Number) r.get("left_start")).intValue(), ((Number) r.get("start")).intValue())))
                .peek(r -> r.put("right", text.substring(((Number) r.get("end")).intValue(), ((Number) r.get("right_end")).intValue())))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> ais(List<Resource> resources, Long featureId, String value, Integer width) throws SQLException, ManagerException {
        width = width == null ? environment.getProperty("search.default-width", Integer.class, 10) : width;
        int maxSize = environment.getProperty("search.max-section-size", Integer.class, 4096);
        if (resources == null || resources.isEmpty()) {
            throw new ManagerException("null or empty resources");
        }
        if (featureId == null) {
            throw new ManagerException("feature id missing");
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (Resource resource : resources) {
            data.addAll(ais(resource, featureId, value, maxSize, width));
        }
        return data;
    }

    private List<Map<String, Object>> ais(Resource resource, Long featureId, String value, int maxSize, int width) throws SQLException, ManagerException {
        StringBuilder builder = new StringBuilder();
        builder.append("select \n")
                .append(" r.id \"resource_id\",\n")
                .append(" r.name \"resource_name\",\n")
                .append(" sl.id \"section_left_id\",\n")
                .append(" sl.index \"section_left_index\",\n")
                .append(" sr.id \"section_right_id\",\n")
                .append(" sr.index \"section_right_index\",\n")
                .append(" rl.id \"row_left_id\",\n")
                .append(" rl.number \"row_left_number\",\n")
                .append(" rr.id \"row_right_id\",\n")
                .append(" rr.number \"row_right_number\",\n")
                .append(" a.start,\n")
                .append(" a.end,\n")
                .append(" sl.start \"left_start\",\n")
                .append(" sr.end \"right_end\"\n")
                .append("from ").append(quote(Annotation.class)).append(" a\n")
                .append("join ").append(quote(AnnotationFeature.class)).append(" af on af.annotation_id = a.id\n")
                .append("left join ").append(quote(Token.class)).append(" tl on tl.resource_id = a.resource_id and tl.start = a.start\n")
                .append("left join ").append(quote(Token.class)).append(" tr on tr.resource_id = a.resource_id and tr.end = a.end\n")
                .append("left join ").append(quote(Row.class)).append(" rl on rl.id = tl.row_id\n")
                .append("left join ").append(quote(Row.class)).append(" rr on rr.id = tr.row_id\n")
                .append("left join ").append(quote(Section.class)).append(" sl on sl.id = rl.section_id\n")
                .append("left join ").append(quote(Section.class)).append(" sr on sr.id = rr.section_id\n")
                .append("join ").append(quote(Resource.class)).append(" r on r.id = a.resource_id\n")
                .append("where a.resource_id = ").append(resource.getId()).append(" and af.feature_id = ").append(featureId).append(" and af.value = '").append(value).append("'\n")
                .append("order by tl.start");
        List<Map<String, Object>> list = databaseManager.query(builder.toString());
        String text = list.isEmpty() ? "" : resourceManager.getText(resource);
        Number sectionStart, sectionEnd;
        for (Map<String, Object> r : list) {
            sectionStart = (Number) r.get("left_start");
            sectionEnd = (Number) r.get("right_end");
            int offset, start, end;
            if (sectionStart == null || sectionEnd == null || sectionEnd.intValue() - sectionStart.intValue() > maxSize) {
                start = ((Number) r.get("start")).intValue();
                offset = getNoSectionOffset(new StringBuilder(text.substring(0, start)).reverse().toString(), width);
                sectionStart = start - offset;
                r.put("left_start", sectionStart);
                end = ((Number) r.get("end")).intValue();
                offset = getNoSectionOffset(text.substring(end, text.length()), width);
                sectionEnd = end + offset;
                r.put("right_end", sectionEnd);
            }
            r.put("section", text.substring(sectionStart.intValue(), sectionEnd.intValue()));
            r.put("annotation", text.substring(((Number) r.get("start")).intValue(), ((Number) r.get("end")).intValue()));
        }
        return list;
    }

    private int getNoSectionOffset(String text, int width) {
        int offset = -1;
        Pattern pattern = Pattern.compile("\s");
        Matcher matcher = pattern.matcher(text);
        while (width > 0 && matcher.find()) {
            width--;
            offset = matcher.end();
        }
        return offset - 1;
    }

    public List<Map<String, Object>> getWordAnnotations(Resource resource, List<Layer> layers, Offset offset) throws SQLException {
        StringBuilder builder = new StringBuilder();
        builder.append("select\n")
                .append(" a.id annotation_id,\n")
                .append(" a.resource_id,\n")
                .append(" r.name \"resource_name\",\n")
                .append(" a.layer_id,\n")
                .append(" l.name \"layer_name\",\n")
                .append(" a.start,\n")
                .append(" a.end,\n")
                .append(" af.feature_id,\n")
                .append(" f.name \"feature_name\",\n")
                .append(" f.type \"feature_type\",\n")
                .append(" af.value \n")
                .append("from ").append(quote(Annotation.class)).append(" a\n")
                .append("join ").append(quote(AnnotationFeature.class)).append(" af on af.annotation_id = a.id\n")
                .append("join ").append(quote(Resource.class)).append(" r on r.id = a.resource_id\n")
                .append("join ").append(quote(Layer.class)).append(" l on l.id = a.layer_id\n")
                .append("join ").append(quote(Feature.class)).append(" f on f.id = af.feature_id\n")
                .append("where resource_id = ").append(resource.getId()).append("\n")
                .append("and a.layer_id in ").append(joiningIds(layers)).append("\n")
                .append("and a.start = ").append(offset.getStart()).append(" and a.end = ").append(offset.getEnd()).append("");
        List<Map<String, Object>> list = databaseManager.query(builder.toString());
        List<Map<String, Object>> ret = new ArrayList<>();
        Map<String, Object> current = null;
        for (Map<String, Object> map : list) {
            if (current == null || !current.get("annotation_id").equals(map.get("annotation_id"))) {
                current = map;
                current.put("features", new ArrayList<Map<String, Object>>());
                ret.add(current);
            }
            ((ArrayList<Map<String, Object>>) current.get("features")).add(Map.of(
                    "feature_id", map.remove("feature_id"),
                    "feature_name", map.remove("feature_name"),
                    "feature_type", map.remove("feature_type"),
                    "value", map.remove("value")));
        }
        return ret;
    }

    private String joiningIds(List<? extends Entity> entities) {
        return entities.stream()
                .map(e -> e.getId().toString())
                .collect(Collectors.joining(",", "(", ")"));
    }

    public List<String> featureValues(List<Resource> resources, String query, Feature feature) throws ManagerException, SQLException {
        if (resources == null || resources.isEmpty()) {
            throw new ManagerException("null or empty resources");
        }
        if (query == null || query.isEmpty()) {
            throw new ManagerException("null or empty query");
        }
        query = query
                .replaceAll("value ", "a.value")
                .replaceAll("token", "a.value");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Resource resource : resources) {
            list.addAll(featureValues(resource, query, feature));
        }
        return list.stream()
                .map(e -> (String) e.get("value"))
                .distinct()
                .toList();
    }

    public List<Map<String, Object>> featureValues(Resource resource, String query, Feature feature) throws ManagerException, SQLException {
        StringBuilder builder = new StringBuilder();
        builder.append("select distinct f.value \n")
                .append("from ").append(quote(Analysis.class)).append(" a\n")
                .append("join ").append(quote(Token.class)).append(" t on t.id = a.token_id\n")
                .append("join ").append(quote(Annotation.class)).append(" n on n.resource_id = t.resource_id and n.start = t.start and n.end = t.end\n")
                .append("join ").append(quote(AnnotationFeature.class)).append(" f on f.annotation_id = n.id\n")
                .append("where a.resource_id = ").append(resource.getId())
                .append(" and (").append(query).append(")")
                .append(" and f.feature_id = ").append(feature.getId());
        return databaseManager.query(builder.toString());
    }

}
