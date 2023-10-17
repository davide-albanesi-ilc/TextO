package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Layer;
import java.sql.SQLException;
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
    public String getLog(Layer layer) throws SQLException, ReflectiveOperationException, ManagerException {
        return layer.getName() != null ? layer.getName() : "" + layer.getId();
    }

}
