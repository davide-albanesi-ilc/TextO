package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.FeatureType;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.FeatureManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("feature")
public class FeatureController extends EntityController<Feature> {

    @Autowired
    private FeatureManager featureManager;

    @Override
    protected Class<Feature> entityClass() {
        return Feature.class;
    }

    @Override
    protected EntityManager<Feature> entityManager() {
        return featureManager;
    }

    @Override
    protected void checkAccess(Feature feature, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        Layer layer = feature.getLayer();
        if (layer != null) {
            accessManager.checkAccess(layer, action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }

    @GetMapping("types")
    public FeatureType[] types() throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get feature types");
        return FeatureType.values();
    }
}
