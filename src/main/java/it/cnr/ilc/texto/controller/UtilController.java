package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.manager.LayerManager;
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
    @Autowired
    private LayerManager layerManager;

    @GetMapping("resource/{id}/sections")
    public List<Map<String, Object>> sections(@PathVariable("id") Long id, @RequestParam(required = false) boolean lazy) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get sections of").appendMessage(Resource.class);
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
        logManager.setMessage("get sections of").appendMessage(Resource.class);
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
        logManager.setMessage("get rows of").appendMessage(Resource.class);
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
    public List<Map<String, Object>> resourceAnnotations(@PathVariable("id") Long id, @RequestBody AnnotationsRequest request) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get annotations on").appendMessage(Resource.class);
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.READ);
        Offset offset = Offset.fromValues(request.start, request.end);
        logManager.appendMessage(offset.toString());
        List<Layer> layers = checkLayers(request.layers);
        return utilManager.getAnnotations(resource, layers, offset);
    }

    public static record AnnotationsRequest(List<Long> layers, Integer start, Integer end) {

    }

    @PostMapping("resource/{id}/word-annotations")
    public List<Map<String, Object>> wordAnnotations(@PathVariable("id") Long id, @RequestBody AnnotationsRequest request) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get annotations on").appendMessage(Resource.class);
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.READ);
        Offset offset = Offset.fromValues(request.start, request.end);
        logManager.appendMessage(offset.toString());
        List<Layer> layers = checkLayers(request.layers);
        return utilManager.getWordAnnotations(resource, layers, offset);
    }

    @PostMapping("kwic")
    public List<Map<String, Object>> kwic(@RequestBody KwicRequest request) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("keyword in context query").appendMessage("\"" + request.query + "\"");
        List<Resource> resources = checkResources(request.resources);
        Layer layer = null;
        if (request.layer != null) {
            if ((layer = layerManager.load(request.layer)) == null) {
                throw new ManagerException("layer not found");
            }
            accessManager.checkAccess(layer, Action.READ);
        }
        KwicRequest requestCache = (KwicRequest) accessManager.getSession().getCache().get("kwicRequest");
        List<Map<String, Object>> data = (List<Map<String, Object>>) accessManager.getSession().getCache().get("kwicData");
        if (requestCache != null && data != null && !request.hasToRelaod(requestCache)) {
            return data;
        } else {
            data = utilManager.kwic(resources, layer, request.query, request.width);
            accessManager.getSession().getCache().put("kwicRequest", request);
            accessManager.getSession().getCache().put("kwicData", data);
            return data;
        }
    }

    public static record KwicRequest(List<Long> resources, Long layer, String query, Integer width, Boolean reload) {

        private boolean hasToRelaod(KwicRequest cache) {
            return cache == null
                    || Boolean.TRUE.equals(this.reload)
                    || (this.resources == null && cache.resources != null)
                    || (this.resources != null && !this.resources.equals(cache.resources))
                    || (this.layer == null && cache.layer != null)
                    || (this.layer != null && !this.layer.equals(cache.layer))
                    || (this.width == null && cache.width != null)
                    || (this.width != null && !this.width.equals(cache.width))
                    || (this.query == null && cache.query != null)
                    || (this.query != null && !this.query.equals(cache.query));
        }
    }

    @PostMapping("aic")
    public List<Map<String, Object>> aic(@RequestBody AicRequest request) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("annotation in context");
        List<Resource> resources = checkResources(request.resources);
        AicRequest requestCache = (AicRequest) accessManager.getSession().getCache().get("aicRequest");
        List<Map<String, Object>> data = (List<Map<String, Object>>) accessManager.getSession().getCache().get("aicData");
        if (requestCache != null && data != null && !request.hasToRelaod(requestCache)) {
            return data;
        } else {
            data = utilManager.aic(resources, request.featureId, request.value, request.width);
            accessManager.getSession().getCache().put("aicRequest", request);
            accessManager.getSession().getCache().put("aicData", data);
            return data;
        }
    }

    public static record AicRequest(List<Long> resources, Long featureId, String value, Integer width, Boolean reload) {

        private boolean hasToRelaod(AicRequest cache) {
            return cache == null
                    || Boolean.TRUE.equals(this.reload)
                    || (this.resources == null && cache.resources != null)
                    || (this.resources != null && !this.resources.equals(cache.resources))
                    || (this.featureId == null && cache.featureId != null)
                    || (this.featureId != null && !this.featureId.equals(cache.featureId))
                    || (this.value == null && cache.value != null)
                    || (this.value != null && !this.value.equals(cache.value))
                    || (this.width == null && cache.width != null)
                    || (this.width != null && !this.width.equals(cache.width));
        }
    }

    @PostMapping("ais")
    public List<Map<String, Object>> ais(@RequestBody AisRequest request) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("annotation in section");
        List<Resource> resources = checkResources(request.resources);
        AisRequest requestCache = (AisRequest) accessManager.getSession().getCache().get("aisRequest");
        List<Map<String, Object>> data = (List<Map<String, Object>>) accessManager.getSession().getCache().get("aisData");
        if (requestCache == null || request.hasToRelaod(requestCache)) {
            data = utilManager.ais(resources, request.featureId, request.value, request.width);
            accessManager.getSession().getCache().put("aisRequest", request);
            accessManager.getSession().getCache().put("aisData", data);
        }
        return data;
    }

    public static record AisRequest(List<Long> resources, Long featureId, String value, Integer width, Boolean reload) {

        private boolean hasToRelaod(AisRequest cache) {
            return cache == null
                    || Boolean.TRUE.equals(this.reload)
                    || (this.resources == null && cache.resources != null)
                    || (this.resources != null && !this.resources.equals(cache.resources))
                    || (this.featureId == null && cache.featureId != null)
                    || (this.featureId != null && !this.featureId.equals(cache.featureId))
                    || (this.value == null && cache.value != null)
                    || (this.value != null && !this.value.equals(cache.value))
                    || (this.width == null && cache.width != null)
                    || (this.width != null && !this.width.equals(cache.width));

        }
    }

    private List<Resource> checkResources(List<Long> ids) throws ReflectiveOperationException, SQLException, ForbiddenException, ManagerException {
        logManager.appendMessage("on").appendMessage(Resource.class);
        List<Resource> resources = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
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
            for (Long id : ids) {
                if ((resource = resourceManager.load(id)) == null) {
                    logManager.appendMessage("" + id);
                    throw new ManagerException("not found");
                } else {
                    resources.add(resource);
                }
            }
        }
        return resources;
    }

    private List<Layer> checkLayers(List<Long> ids) throws ReflectiveOperationException, SQLException, ForbiddenException, ManagerException {
        logManager.appendMessage("of").appendMessage(Layer.class);
        List<Layer> layers = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            for (Layer layer : layerManager.load()) {
                try {
                    accessManager.checkAccess(layer, Action.READ);
                    layers.add(layer);
                } catch (ForbiddenException e) {
                }
            }
            if (layers.isEmpty()) {
                throw new ForbiddenException();
            }
        } else {
            Layer layer;
            for (Long id : ids) {
                if ((layer = layerManager.load(id)) == null) {
                    logManager.appendMessage("" + id);
                    throw new ManagerException("not found");
                } else {
                    layers.add(layer);
                }
            }
        }
        return layers;
    }
}
