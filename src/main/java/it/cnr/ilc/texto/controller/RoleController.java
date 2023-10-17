package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Role;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.RoleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("role")
public class RoleController extends EntityController<Role> {

    @Autowired
    private RoleManager roleManager;

    @Override
    protected Class<Role> entityClass() {
        return Role.class;
    }

    @Override
    protected EntityManager<Role> entityManager() {
        return roleManager;
    }

}
