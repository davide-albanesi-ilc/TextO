package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
public class AnnotationManager extends EntityManager<Annotation> {

    @Autowired
    private AccessManager accessManager;
    @Autowired
    private ResourceManager resourceManager;
    @Lazy
    @Autowired
    private AnnotationFeatureManager annotationFeatureManager;

    @Override
    protected Class<Annotation> entityClass() {
        return Annotation.class;
    }

    @Override
    public String getLog(Annotation annotation) {
        return (annotation.getLayer() != null ? annotation.getLayer().getName() : "" + annotation.getId()) + "[" + annotation.getStart() + "-" + annotation.getEnd() + "]";
    }

    @Trigger(event = Trigger.Event.PRE_REMOVE)
    public void removeAnnotationFeatures(Annotation previous, Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        annotationFeatureManager.removeAnnotationFeatures(annotation);
    }

    public List<Map<String, Object>> getAnnotations(Resource resource, Offset offset) throws SQLException, ReflectiveOperationException, ManagerException {
        resourceManager.checkOffset(offset, resourceManager.getCharacterCount(resource));
        Map<Long, Map<String, Object>> laysers = loadLayers(resource, offset.start, offset.end);
        Map<Long, Map<String, Object>> features = loadFeatures(laysers);
        StringBuilder sql = new StringBuilder();
        sql.append("select a.id, a.resource_id \"resource\", a.layer_id \"layer\", a.start, a.end, a.user_id \"user\", af.id \"af.id\", af.feature_id, af.value ")
                .append("from Annotation a join AnnotationFeature af on af.annotation_id = a.id and af.status = 1 and a.status = 1 ")
                .append("where a.resource_id = ").append(resource.getId()).append(" and a.start >= ").append(offset.start).append(" and a.end < ").append(offset.end);
        List<Map<String, Object>> annotations = databaseManager.query(sql.toString());
        List<Map<String, Object>> annotationFeatures;
        Map<String, Object> annotationFeature;
        Map<String, Object> previous = null;
        List<Map<String, Object>> returns = new ArrayList<>(annotations.size());
        for (Map<String, Object> annotation : annotations) {
            if (previous == null || !previous.get("id").equals(annotation.get("id"))) {
                annotation.put("resource", Map.of("id", annotation.get("resource")));
                annotation.put("layer", laysers.get((Long) annotation.get("layer")));
                annotation.put("user", Map.of("id", annotation.get("user")));
                annotationFeatures = new ArrayList<>();
                annotationFeature = new LinkedHashMap<>();
                annotationFeature.put("id", annotation.remove("af.id"));
                annotationFeature.put("annotation", Map.of("id", annotation.get("id")));
                annotationFeature.put("feature", features.get((Long) annotation.remove("feature_id")));
                annotationFeature.put("value", annotation.remove("value"));
                annotationFeatures.add(annotationFeature);
                annotation.put("features", annotationFeatures);
                returns.add(annotation);
                previous = annotation;
            } else {
                annotationFeatures = (List<Map<String, Object>>) previous.get("features");
                annotationFeature = new LinkedHashMap<>();
                annotationFeature.put("id", annotation.remove("af.id"));
                annotationFeature.put("annotation", Map.of("id", annotation.get("id")));
                annotationFeature.put("feature", features.get((Long) annotation.remove("feature_id")));
                annotationFeature.put("value", annotation.remove("value"));
                annotationFeatures.add(annotationFeature);
            }
        }
        return returns;
    }

    private Map<Long, Map<String, Object>> loadLayers(Resource resource, Integer start, Integer end) throws SQLException, ReflectiveOperationException, ManagerException {
        Map<Long, Map<String, Object>> layers = new HashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("select distinct l.id, l.name, l.description, l.color, l.overlapping from Annotation a join Layer l on l.id = a.layer_id and l.status = 1 ")
                .append("where a.status = 1 and a.resource_id = ").append(resource.getId()).append(" and a.start >= ").append(start).append(" and a.end < ").append(end);
        List<Map<String, Object>> query = databaseManager.query(sql.toString());
        Layer layer;
        for (Map<String, Object> record : query) {
            layer = Entity.newGhost(Layer.class, ((Number) record.get("id")).longValue());
            try {
                accessManager.checkAccess(layer, Action.READ);
                layers.put(layer.getId(), record);
            } catch (ForbiddenException ex) {
            }
        }
        return layers;
    }

    private Map<Long, Map<String, Object>> loadFeatures(Map<Long, Map<String, Object>> layers) throws SQLException, ReflectiveOperationException, ManagerException {
        if (layers.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        Map<Long, Map<String, Object>> features = new HashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("select id, name, description, layer_id \"layer\", type, tagset_id \"tagset\" from Feature where status = 1 and layer_id in (")
                .append(layers.keySet().stream().map(l -> l.toString()).collect(Collectors.joining(", "))).append(")");
        List<Map<String, Object>> query = databaseManager.query(sql.toString());
        Number tagset;
        for (Map<String, Object> record : query) {
            record.put("layer", Map.of("id", record.get("layer")));
            if ((tagset = (Number) record.get("tagset")) != null) {
                record.put("tagset", Map.of("id", tagset));
            }
            features.put(((Number) record.get("id")).longValue(), record);
        }
        return features;
    }

}
