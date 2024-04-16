package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.SectionManager;
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
@RequestMapping("section")
public class SectionController extends EntityController<Section> {

    @Autowired
    private SectionManager sectionManager;

    @Override
    protected EntityManager<Section> entityManager() {
        return sectionManager;
    }

    @Override
    protected Class<Section> entityClass() {
        return Section.class;
    }

    @Override
    protected void checkAccess(Section section, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (section.getResource() != null) {
            accessManager.checkAccess(section.getResource(), action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }

    @GetMapping("{id}/sections")
    public List<Section> sections(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get sections of " + entityClass().getSimpleName());
        Section section = sectionManager.load(id);
        if (section == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(sectionManager.getLog(section));
        checkAccess(section, Action.READ);
        return sectionManager.load(section);
    }
}
