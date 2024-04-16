package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.manager.ResourceManager;
import it.cnr.ilc.texto.manager.SearchManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("search")
public class SearchController extends Controller {

    @Autowired
    private ResourceManager resourceManager;
    @Autowired
    private SearchManager searchManager;

    @PostMapping("kwic")
    public List<Map<String, Object>> kwic(@RequestBody KwicRequest request) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("kwic query").appendMessage("\"" + request.query + "\"");
        if (request.resources == null || request.resources.isEmpty()) {
            Set<Resource> resources = new HashSet<>();
            for (Resource resource : resourceManager.load()) {
                try {
                    accessManager.checkAccess(resource, Action.READ);
                    resources.add(resource);
                } catch (ForbiddenException e) {
                }
            }
            if (resources.isEmpty()) {
                throw new ForbiddenException();
            }
            return searchManager.kwic(resources, request.query, request.width);
        } else {
            for (Resource resource : request.resources) {
                if (resourceManager.load(resource.getId()) == null) {
                    logManager.appendMessage("" + resource.getId());
                    throw new ManagerException("not found");
                }
                accessManager.checkAccess(resource, Action.READ);
            }
            return searchManager.kwic(request.resources, request.query, request.width);
        }
    }

    public static record KwicRequest(Set<Resource> resources, String query, Integer width) {

    }
}
