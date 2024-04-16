package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.manager.AnnotationFeatureManager;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("annotationFeature")
public class AnnotationFeatureController extends EntityController<AnnotationFeature> {

    @Autowired
    private AnnotationController annotationController;
    @Autowired
    private AnnotationFeatureManager annotationFeatureManager;

    @Override
    protected Class<AnnotationFeature> entityClass() {
        return AnnotationFeature.class;
    }

    @Override
    protected EntityManager<AnnotationFeature> entityManager() {
        return annotationFeatureManager;
    }

    @Override
    protected void checkAccess(AnnotationFeature annotationFeature, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (annotationFeature.getAnnotation() != null) {
            annotationController.checkAccess(annotationFeature.getAnnotation(), action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }

}
