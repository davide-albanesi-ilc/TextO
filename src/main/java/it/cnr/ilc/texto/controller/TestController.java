package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Test;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.TestManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("{id}/test")
    public String test(@PathVariable("id") Long id, @RequestParam(required = false, name = "tt") String tt, @RequestBody String content) {
        return "test";
    }

}
