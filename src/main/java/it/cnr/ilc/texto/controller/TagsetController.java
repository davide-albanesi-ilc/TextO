package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.TagsetManager;
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
@RequestMapping("tagset")
public class TagsetController extends EntityController<Tagset> {

    @Autowired
    private TagsetManager tagsetManager;
    @Override
    protected Class<Tagset> entityClass() {
        return Tagset.class;
    }

    @Override
    protected EntityManager<Tagset> entityManager() {
        return tagsetManager;
    }

    @GetMapping("{id}/items")
    public List<TagsetItem> items(@PathVariable("id") Long id) throws ManagerException, SQLException, ReflectiveOperationException, ForbiddenException {
        logManager.setMessage("get items of").appendMessage(entityClass().getSimpleName());
        Tagset tagset = tagsetManager.load(id);
        if (tagset == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(tagsetManager.getLog(tagset));
        accessManager.checkAccess(tagset, Action.READ);
        return tagsetManager.getItems(tagset);
    }
}
