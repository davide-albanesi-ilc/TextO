package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.TagsetItemManager;
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
@RequestMapping("tagsetItem")
public class TagsetItemController extends EntityController<TagsetItem> {

    @Autowired
    private TagsetItemManager tagsetItemManager;

    @Override
    protected Class<TagsetItem> entityClass() {
        return TagsetItem.class;
    }

    @Override
    protected EntityManager<TagsetItem> entityManager() {
        return tagsetItemManager;
    }

    @Override
    protected void checkAccess(TagsetItem tagsetItem, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (tagsetItem.getTagset() != null) {
            accessManager.checkAccess(tagsetItem.getTagset(), action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }
}
