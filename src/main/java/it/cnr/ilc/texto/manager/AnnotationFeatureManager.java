package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.Tagset;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
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
    @Lazy
    @Autowired
    private AnalysisManager analysisManager;

    @Override
    protected Class<AnnotationFeature> entityClass() {
        return AnnotationFeature.class;
    }

    @Trigger(event = Trigger.Event.PRE_CREATE)
    @Trigger(event = Trigger.Event.PRE_REMOVE)
    public void checkAnalysis(AnnotationFeature previous, AnnotationFeature annotationFeature) throws ManagerException {
        if (analysisManager.isAnalysisFeature(annotationFeature.getFeature())) {
            throw new ManagerException("analysis is locked");
        }
    }

    @Trigger(event = Trigger.Event.PRE_UPDATE)
    public void checkAnalysisUpdate(AnnotationFeature previous, AnnotationFeature annotationFeature) throws SQLException, ReflectiveOperationException, ManagerException {
        if (analysisManager.isAnalysisFeature(annotationFeature.getFeature())) {
            if (analysisManager.getLemmaFeatures().get("Lemma").equals(annotationFeature.getFeature())) {
                analysisManager.updateLemma(previous, annotationFeature);
            } else if (analysisManager.getPosFeatures().get("UPOS").equals(annotationFeature.getFeature())) {
                analysisManager.updateUPOS(previous, annotationFeature);
            } else {
                throw new ManagerException("analysis is locked");
            }
        }
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
                .append(" where annotation_id = ").append(annotation.getId());
        return load(sql.toString());
    }

    public void remove(Annotation annotation) throws SQLException, ReflectiveOperationException, ManagerException {
        for (AnnotationFeature annotationFeature : load(annotation)) {
            remove(annotationFeature);
        }
    }

    public void tagsetValueMultipleUpdate(Tagset tagset, String previous, String current) throws SQLException, ManagerException {
        if (Objects.equals(previous, current)) {
            throw new ManagerException("no changes");
        } else {
            StringBuilder sql = new StringBuilder();
            sql.append("update ").append(quote(AnnotationFeature.class)).append("\n")
                    .append("set value = '").append(current).append("'\n")
                    .append("where feature_id in (select id from ").append(quote(Feature.class)).append(" where tagset_id = ").append(tagset.getId()).append(")\n")
                    .append("and value = '").append(previous).append("'");
            databaseManager.update(sql.toString());
        }
    }
}
