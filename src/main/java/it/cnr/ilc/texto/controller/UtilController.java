package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.manager.ResourceManager;
import it.cnr.ilc.texto.manager.SectionManager;
import it.cnr.ilc.texto.manager.UtilManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
@RequestMapping("util")
public class UtilController extends Controller {

    @Autowired
    private UtilManager utilManager;
    @Autowired
    private ResourceManager resourceManager;
    @Autowired
    private SectionManager sectionManager;

    @GetMapping("resource/{id}/sections")
    public List<Map<String, Object>> sections(@PathVariable("id") Long id, @RequestParam(required = false) boolean lazy) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get sections of " + Resource.class.getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.READ);
        return lazy ? utilManager.getSections(resource, null) : utilManager.getSections(resource);
    }

    @GetMapping("section/{id}/sections")
    public List<Map<String, Object>> sections(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get sections of " + Resource.class.getSimpleName());
        Section section = sectionManager.load(id);
        if (section == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(sectionManager.getLog(section));
        accessManager.checkAccess(section.getResource(), Action.READ);
        return utilManager.getSections(section.getResource(), section);
    }

    @PostMapping("resource/{id}/rows")
    public Map<String, Object> rows(@PathVariable("id") Long id, @RequestBody Offset offset) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get rows of " + Resource.class.getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        logManager.appendMessage(offset.toString());
        accessManager.checkAccess(resource, Action.READ);
        return utilManager.getRows(resource, offset);
    }

    @PostMapping("resource/{id}/annotations")
    public List<Map<String, Object>> annotations(@PathVariable("id") Long id, @RequestBody AnnotationsRequest request) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get annotations of " + Resource.class.getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        if (request.layers != null) {
            logManager.appendMessage(request.layers.toString());
        }
        Offset offset = Offset.fromValues(request.start, request.end);
        logManager.appendMessage(offset.toString());
        accessManager.checkAccess(resource, Action.READ);
        return utilManager.getAnnotations(resource, request.layers, offset);
    }

    public static record AnnotationsRequest(List<Long> layers, Integer start, Integer end) {

    }

    @PostMapping("kwic")
    public List<Map<String, Object>> kwic(@RequestBody KwicRequest request) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("kwic query").appendMessage("\"" + request.query + "\"");
        List<Resource> resources = new ArrayList<>();
        if (request.resources == null || request.resources.isEmpty()) {
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
        } else {
            Resource resource;
            for (Long id : request.resources) {
                if ((resource = resourceManager.load(id)) == null) {
                    logManager.appendMessage("" + id);
                    throw new ManagerException("not found");
                } else {
                    resources.add(resource);
                }
            }
        }
        KwicRequest requestCache = (KwicRequest) accessManager.getSession().getCache().get("kwicRequest");
        List<Map<String, Object>> data = (List<Map<String, Object>>) accessManager.getSession().getCache().get("kwicData");
        if (requestCache != null && data != null && !request.hasToRelaod(requestCache)) {
            return data;
        } else {
            data = utilManager.kwic(resources, request.query, request.width);
            accessManager.getSession().getCache().put("kwicRequest", request);
            accessManager.getSession().getCache().put("kwicData", data);
            return data;
        }
    }

    public static record KwicRequest(List<Long> resources, String query, Integer width, Boolean reload) {

        private boolean hasToRelaod(KwicRequest cache) {
            return cache == null
                    || Boolean.TRUE.equals(this.reload)
                    || (this.resources == null && cache.resources != null)
                    || (this.resources != null && !this.resources.equals(cache.resources))
                    || (this.width == null && cache.width != null)
                    || (this.width != null && !this.width.equals(cache.width))
                    || (this.query == null && cache.query != null)
                    || (this.query != null && !this.query.equals(cache.query));
        }
    }

}
