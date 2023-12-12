package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Test;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.TestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("test")
public class TestController extends EntityController<Test> {

    @Autowired
    private TestManager roleManager;

    @Override
    protected Class<Test> entityClass() {
        return Test.class;
    }

    @Override
    protected EntityManager<Test> entityManager() {
        return roleManager;
    }

    @GetMapping("para")
    public String test(@RequestParam("id") String id, @RequestParam(required = false, name = "tt") String tt) {
        System.out.println("id = " + id);
        System.out.println("tt = " + tt);
        return id;
    }

}
