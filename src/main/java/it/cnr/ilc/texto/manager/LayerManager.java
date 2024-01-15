package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Layer;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class LayerManager extends EntityManager<Layer> {

    @Override
    protected Class<Layer> entityClass() {
        return Layer.class;
    }

    @Override
    public String getLog(Layer layer) {
        return layer.getName() != null ? layer.getName() : "" + layer.getId();
    }

}
