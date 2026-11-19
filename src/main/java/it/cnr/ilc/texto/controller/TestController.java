package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Test;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.TestManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
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

    @GetMapping("")
    public void test() {
        logManager.setMessage("test");
    }

    @PostMapping("{id}/test")
    public Map<String, Object> test(
            @PathVariable("id") Long id,
            @RequestParam(required = true, name = "para") String para,
            @RequestParam(required = false, name = "error") String error,
            @RequestBody Map<String, Object> content) throws ManagerException, ForbiddenException {

        logManager.appendMessage("test on " + id);
        if ("forbidden".equals(error)) {
            throw new ForbiddenException();
        }
        if ("manager".equals(error)) {
            throw new ManagerException("manager error");
        }
        if ("runtime".equals(error)) {
            throw new RuntimeException("runtime error");
        }
        Map<String, Object> map = new TreeMap<>();
        map.put("id", id);
        map.put("para", para);
        map.put("error", error);
        return map;
    }
}
