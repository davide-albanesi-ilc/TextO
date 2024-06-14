package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Status;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
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
public class AnnotationFeatureManager extends EntityManager<AnnotationFeature> {

    @Lazy
    @Autowired
    private AnnotationManager annotationManager;
    @Lazy
    @Autowired
    private FeatureManager featureManager;

    @Override
    protected Class<AnnotationFeature> entityClass() {
        return AnnotationFeature.class;
    }

    @Override
    public String getLog(AnnotationFeature annotationFeature) throws SQLException, ReflectiveOperationException, ManagerException {
        if (annotationFeature.getAnnotation() == null || annotationFeature.getFeature() == null || annotationFeature.getValue() == null) {
            return "" + annotationFeature.getId();
        } else {
            return annotationManager.getLog(annotationManager.load(annotationFeature.getAnnotation().getId())) + " "
                    + featureManager.getLog(featureManager.load(annotationFeature.getFeature().getId())) + " "
                    + annotationFeature.getValue();
        }
    }

    public List<AnnotationFeature> load(Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(AnnotationFeature.class))
                .append(" where status = ").append(Status.VALID.ordinal())
                .append(" and annotation_id = ").append(annotation.getId());
        return load(sql.toString());
    }

    public void remove(Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        for (AnnotationFeature annotationFeature : load(annotation)) {
            remove(annotationFeature);
        }
    }
}
