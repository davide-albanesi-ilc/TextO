package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.annotation.Trigger.Event;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class LayerManager extends EntityManager<Layer> {

    @Lazy
    @Autowired
    private FeatureManager featureManager;

    @Override
    protected Class<Layer> entityClass() {
        return Layer.class;
    }

    @Override
    public String getLog(Layer layer) {
        return layer.getName() != null ? layer.getName() : "" + layer.getId();
    }

    @Trigger(event = Event.PRE_REMOVE)
    public void removeFeatures(Layer previous, Layer layer) throws SQLException, ReflectiveOperationException, ManagerException {
        featureManager.remove(layer);
    }
}
