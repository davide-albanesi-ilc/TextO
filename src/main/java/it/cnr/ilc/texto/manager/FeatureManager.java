package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.FeatureType;
import it.cnr.ilc.texto.domain.Layer;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.annotation.Trigger;
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
public class FeatureManager extends EntityManager<Feature> {

    @Lazy
    @Autowired
    private TagsetManager tagsetManager;
    @Lazy
    @Autowired
    private LayerManager layerManager;
    @Lazy
    @Autowired
    private AnalysisManager analysisManager;

    @Override
    protected Class<Feature> entityClass() {
        return Feature.class;
    }

    @Override
    public String getLog(Feature feature) throws SQLException, ReflectiveOperationException, ManagerException {
        if (feature.getLayer() == null || feature.getName() == null) {
            return "" + feature.getId();
        } else {
            return layerManager.getLog(layerManager.load(feature.getLayer().getId())) + " "
                    + feature.getName();
        }
    }

    @Trigger(event = Trigger.Event.PRE_UPDATE)
    @Trigger(event = Trigger.Event.PRE_REMOVE)
    public void checkAnalysis(Feature previous, Feature feature) throws ManagerException {
        if (analysisManager.isAnalysisFeature(feature)) {
            throw new ManagerException("analysis is locked");
        }
    }
    
    @Check
    protected void checkTagset(Feature previous, Feature feature) throws ManagerException {
        if (FeatureType.TAGSET.equals(feature.getType()) && feature.getTagset() == null) {
            throw new ManagerException("tagset required");
        }
    }

    public List<Feature> load(Layer layer) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Feature.class))
                .append(" where layer_id = ").append(layer.getId());
        List<Feature> features = load(sql.toString());
        for (Feature feature : features) {
            if (feature.getTagset() != null) {
                feature.setTagset(tagsetManager.load(feature.getTagset().getId()));
            }
        }
        return features;
    }

    public void remove(Layer layer) throws SQLException, ReflectiveOperationException, ManagerException {
        for (Feature feature : load(layer)) {
            remove(feature);
        }
    }

}
