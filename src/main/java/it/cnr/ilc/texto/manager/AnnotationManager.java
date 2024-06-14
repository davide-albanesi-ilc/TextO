package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Status;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.ResourceManager.checkOffset;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.annotation.Trigger.Event;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
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
                .append(" where status = ").append(Status.VALID.ordinal())
                .append(" and resource_id = ").append(resource.getId())
                .append(" and start >= ").append(offset.start)
                .append(" and end < ").append(offset.end)
                .append(" order by start");
        return load(sql.toString());
    }

}
