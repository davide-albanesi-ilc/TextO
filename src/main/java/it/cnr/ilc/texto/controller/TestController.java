package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Test;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.TestManager;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
    private TestManager testManager;

    @Override
    protected Class<Test> entityClass() {
        return Test.class;
    }

    @Override
    protected EntityManager<Test> entityManager() {
        return testManager;
    }

    @PostMapping("para/{id}")
    public String para(@PathVariable("id") Long id, @RequestParam(required = false, name = "param") Boolean param, @RequestBody String content) {
        return "id: " + id + "\ncontent: " + content + "\nparam:" + param;
    }

    @GetMapping("para")
    public String para(@RequestParam Map<String, String> params) {
        return params.toString();
    }

}
