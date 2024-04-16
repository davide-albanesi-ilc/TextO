package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.SectionType;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.SectionTypeManager;
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
@RequestMapping("sectionType")
public class SectionTypeController extends EntityController<SectionType> {

    @Autowired
    private SectionTypeManager sectionTypeManager;

    @Override
    protected EntityManager<SectionType> entityManager() {
        return sectionTypeManager;
    }

    @Override
    protected Class<SectionType> entityClass() {
        return SectionType.class;
    }

    @Override
    protected void checkAccess(SectionType sectionType, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (sectionType.getResource() != null) {
            accessManager.checkAccess(sectionType.getResource(), action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }

}
