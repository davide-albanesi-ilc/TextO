package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.FeatureManager;
import it.cnr.ilc.texto.manager.LayerManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("layer")
public class LayerController extends EntityController<Layer> {

    @Autowired
    private LayerManager layerManager;
    @Autowired
    private FeatureManager featureManager;

    @Override
    protected Class<Layer> entityClass() {
        return Layer.class;
    }

    @Override
    protected EntityManager<Layer> entityManager() {
        return layerManager;
    }

    @GetMapping("{id}/features")
    public List<Feature> features(@PathVariable("id") long id) throws ManagerException, SQLException, ReflectiveOperationException {
        logManager.setMessage("get features of").appendMessage(Layer.class.getSimpleName());
        Layer layer = layerManager.load(id);
        if (layer == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(layer));
        return featureManager.load(layer);
    }

    @PostMapping("import")
    public void importLayers(@RequestBody String content) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        accessManager.checkAccess(Layer.class, Action.WRITE);
        logManager.setMessage("import layers");
        layerManager.importLayers(content.lines().toList());
    }
    
}
