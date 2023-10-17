package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Group;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.GroupManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("group")
public class GroupController extends EntityController<Group> {

    @Autowired
    private GroupManager groupManager;

    @Override
    protected Class<Group> entityClass() {
        return Group.class;
    }

    @Override
    protected EntityManager<Group> entityManager() {
        return groupManager;
    }

}
