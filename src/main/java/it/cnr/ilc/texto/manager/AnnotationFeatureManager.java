package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
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
        if (annotationFeature.getAnnotation() == null || annotationFeature.getFeature() == null) {
            return "" + annotationFeature.getId();
        } else {
            return annotationManager.getLog(annotationFeature.getAnnotation()) + " " + featureManager.getLog(annotationFeature.getFeature()) + " " + annotationFeature.getValue();
        }
    }

    public List<AnnotationFeature> getAnnotationFeatures(Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(AnnotationFeature.class))
                .append(" where status = 1")
                .append(" and annotation_id = ").append(annotation.getId());
        return load(sql.toString());
    }

    public void removeAnnotationFeatures(Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        for (AnnotationFeature annotationFeature : getAnnotationFeatures(annotation)) {
            remove(annotationFeature);
        }
    }
}
