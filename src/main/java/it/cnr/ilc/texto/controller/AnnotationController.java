package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.manager.AnnotationFeatureManager;
import it.cnr.ilc.texto.manager.AnnotationManager;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("annotation")
public class AnnotationController extends EntityController<Annotation> {

    @Autowired
    private AnnotationManager annotationManager;
    @Autowired
    private AnnotationFeatureManager annotationFeatureManager;

    @Override
    protected Class<Annotation> entityClass() {
        return Annotation.class;
    }

    @Override
    protected EntityManager<Annotation> entityManager() {
        return annotationManager;
    }

    @Override
    protected void preRemove(Annotation annotation) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        annotationFeatureManager.removeAnnotationFeatures(annotation);
    }

    @Override
    protected void checkAccess(Annotation annotation, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (action.equals(Action.CREATE) || action.equals(Action.REMOVE)) {
            accessManager.checkAccess(annotation, action);
        }
        Layer layer = annotation.getLayer();
        if (layer != null) {
            accessManager.checkAccess(layer, action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
        Resource resource = annotation.getResource();
        if (resource != null) {
            accessManager.checkAccess(resource, action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }

    @GetMapping("{id}/features")
    public List<AnnotationFeature> features(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get rows of " + entityClass().getSimpleName());
        Annotation annotation = annotationManager.load(id);
        if (annotation == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(annotationManager.getLog(annotation));
        checkAccess(annotation, Action.READ);
        return annotationFeatureManager.getAnnotationFeatures(annotation);
    }
}