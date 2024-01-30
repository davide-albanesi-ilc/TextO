package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.FeatureType;
import it.cnr.ilc.texto.domain.Layer;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
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
public class FeatureManager extends EntityManager<Feature> {

    @Lazy
    @Autowired
    private TagsetManager tagsetManager;

    @Override
    protected Class<Feature> entityClass() {
        return Feature.class;
    }

    @Override
    public String getLog(Feature feature) {
        return feature.getName() != null ? feature.getName() : "" + feature.getId();
    }

    @Trigger(event = Event.PRE_CREATE)
    @Trigger(event = Event.PRE_UPDATE)
    @Trigger(event = Event.PRE_REMOVE)
    protected void checkTagset(Feature previous, Feature feature) throws ManagerException {
        if (FeatureType.TAGSET.equals(feature.getType()) && feature.getTagset() == null) {
            throw new ManagerException("tagset required");
        }
    }

    public List<Feature> load(Layer layer) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Feature.class))
                .append(" where status = 1")
                .append(" and layer_id = ").append(layer.getId());
        List<Feature> features = load(sql.toString());
        for (Feature feature : features) {
            if (feature.getTagset() != null) {
                feature.setTagset(tagsetManager.load(feature.getTagset().getId()));
            }
        }
        return features;
    }

}
