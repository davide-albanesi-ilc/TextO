package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.ResourceManager.checkOffset;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.annotation.Trigger.Event;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Lazy
    @Autowired
    private AnnotationFeatureManager annotationFeatureManager;
    @Lazy
    @Autowired
    private LayerManager layerManager;
    @Lazy
    @Autowired
    private FeatureManager featureManager;
    @Lazy
    @Autowired
    private ResourceManager resourceManager;

    @Override
    protected Class<Annotation> entityClass() {
        return Annotation.class;
    }

    @Override
    public String getLog(Annotation annotation) throws SQLException, ReflectiveOperationException {
        if (annotation.getLayer() == null || annotation.getResource() == null || annotation.getStart() == null || annotation.getEnd() == null) {
            return "" + annotation.getId();
        } else {
            return layerManager.getLog(layerManager.load(annotation.getLayer().getId())) + " "
                    + resourceManager.getLog(resourceManager.load(annotation.getResource().getId())) + " "
                    + "[" + annotation.getStart() + "-" + annotation.getEnd() + "]";
        }
    }

    @Trigger(event = Event.PRE_REMOVE)
    public void removeAnnotationFeatures(Annotation previous, Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        annotationFeatureManager.remove(annotation);
    }

    public List<Annotation> load(Resource resource, Offset offset) throws SQLException, ReflectiveOperationException, ManagerException {
        checkOffset(offset, resourceManager.getCharacterCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Annotation.class))
                .append(" where resource_id = ").append(resource.getId())
                .append(" and start >= ").append(offset.start)
                .append(" and end < ").append(offset.end)
                .append(" order by start");
        return load(sql.toString());
    }

    public void importAnnotations(Resource resource, List<String> lines, User user) throws SQLException, ReflectiveOperationException, ManagerException {
        Map<String, Layer> layers = layerManager.load().stream().collect(Collectors.toMap(l -> l.getName(), l -> l));
        Map<String, Map<String, Feature>> features = new HashMap<>();
        for (Layer layer : layers.values()) {
            features.put(layer.getName(), featureManager.load(layer).stream().collect(Collectors.toMap(f -> f.getName(), f -> f)));
        }
        List<Entity> entities = new ArrayList<>();
        Annotation annotation = null;
        AnnotationFeature annotationFeature;
        String[] split;
        for (String line : lines) {
            split = line.split("\t");
            if (split[0].equals(Annotation.class.getSimpleName())) {
                annotation = new Annotation();
                annotation.setResource(resource);
                annotation.setLayer(layers.get(split[1]));
                annotation.setStart(Integer.valueOf(split[2]));
                annotation.setEnd(Integer.valueOf(split[3]));
                annotation.setUser(user);
                entities.add(annotation);
            } else if (split[0].equals(AnnotationFeature.class.getSimpleName())) {
                annotationFeature = new AnnotationFeature();
                annotationFeature.setAnnotation(annotation);
                annotationFeature.setFeature(features.get(annotation.getLayer().getName()).get(split[1]));
                annotationFeature.setValue(split[2]);
                entities.add(annotationFeature);
            }
        }
        domainManager.create(entities);
    }

}
