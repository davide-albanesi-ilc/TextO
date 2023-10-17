package it.cnr.ilc.texto.manager;

import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class AnnotationFeatureManager extends EntityManager<AnnotationFeature> {

    @Autowired
    private AnnotationManager annotationManager;
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
                .append(" where status = 1 and annotation_id = ").append(annotation.getId());
        return load(sql.toString());
    }

    public void removeAnnotationFeatures(Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        for (AnnotationFeature annotationFeature : getAnnotationFeatures(annotation)) {
            remove(annotationFeature);
        }
    }
}
