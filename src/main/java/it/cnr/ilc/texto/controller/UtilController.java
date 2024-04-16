package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.DomainManager;
import it.cnr.ilc.texto.manager.ResourceManager;
import it.cnr.ilc.texto.manager.SectionManager;
import it.cnr.ilc.texto.manager.UtilManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private DomainManager domainManager;
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
    public List<Map<String, Object>> annotations(@PathVariable("id") Long id, @RequestBody Offset offset) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get annotations of " + Resource.class.getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        logManager.appendMessage(offset.toString());
        accessManager.checkAccess(resource, Action.READ);
        return utilManager.getAnnotations(resource, offset);
    }

    @PostMapping("kwic")
    public KwicResponse kwic(@RequestBody KwicRequest request) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("kwic query").appendMessage("\"" + request.query + "\"").appendMessage("filter").appendMessage("\"" + request.filter + "\"");
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
        List<Map<String, Object>> data = doKwic(resources, request);
        Offset offset = Offset.fromValues(request.start, request.end);
        checkOffset(offset, data.size());
        List<Map<String, Object>> subData = data.subList(offset.start, offset.end);
        return new KwicResponse(data.size(), offset.start, offset.end, subData);
    }

    private static int checkOffset(Offset offset, int length) throws SQLException, ManagerException {
        if (offset.start == null) {
            offset.start = 0;
        }
        if (offset.end == null || offset.end > length) {
            offset.end = length;
        }
        if (offset.start < 0 || offset.start > length || offset.end < 0 || offset.start > offset.end) {
            throw new ManagerException("index out of bounds");
        }
        return length;
    }

    private List<Map<String, Object>> doKwic(List<Resource> resources, KwicRequest request) throws SQLException, ManagerException {
        if (request.query == null || request.query.isEmpty()) {
            throw new ManagerException("empty query");
        }
        KwicRequest requestCache = (KwicRequest) accessManager.getSession().getCache().get("kwicRequest");
        List<Map<String, Object>> data = (List<Map<String, Object>>) accessManager.getSession().getCache().get("kwicData");
        if (requestCache != null && data != null && !request.hasToRelaod(requestCache)) {
            return data;
        } else {
            data = utilManager.kwic(resources, request.query, request.filter, request.width);
            accessManager.getSession().getCache().put("kwicRequest", request);
            accessManager.getSession().getCache().put("kwicData", data);
            return data;
        }
    }

    public static record KwicRequest(List<Long> resources, String query, String filter, Integer width, Integer start, Integer end, Boolean reload) {

        private boolean hasToRelaod(KwicRequest cache) {
            return cache == null
                    || Boolean.TRUE.equals(this.reload)
                    || (this.resources == null && cache.resources != null)
                    || (this.resources != null && !this.resources.equals(cache.resources))
                    || (this.width == null && cache.width != null)
                    || (this.width != null && !this.width.equals(cache.width))
                    || (this.query == null && cache.query != null)
                    || (this.query != null && !this.query.equals(cache.query))
                    || (this.filter == null && cache.filter != null)
                    || (this.filter != null && !this.filter.equals(cache.filter));
        }
    }

    public static record KwicResponse(Integer count, Integer start, Integer end, List<Map<String, Object>> data) {

    }

    @PostMapping("resource/{id}/loadstress")
    public void loadstress(@PathVariable("id") Long id, @RequestBody Offset offset) throws SQLException, ReflectiveOperationException, IOException, ManagerException {
        Resource resource = domainManager.load(Resource.class, id);
        if (resource == null) {
            throw new ManagerException("resource not found");
        }
        User user = accessManager.getUser();
        Random random = new Random(System.currentTimeMillis());
        List<Layer> layers = domainManager.load(Layer.class);
        Map<Layer, List<Feature>> features = new HashMap<>();
        for (Layer layer : layers) {
            features.put(layer, domainManager.load(Feature.class, "select * from Feature where status = 1 and layer_id = " + layer.getId()));
        }
        List<Tagset> tagsets = domainManager.load(Tagset.class);
        Map<Tagset, List<TagsetItem>> tagsetItems = new HashMap<>();
        for (Tagset tagset : tagsets) {
            tagsetItems.put(tagset, domainManager.load(TagsetItem.class, "select * from TagsetItem where status = 1 and tagset_id = " + tagset.getId()));
        }
        Layer layer;
        Annotation annotation;
        AnnotationFeature annotationFeature;
        List<TagsetItem> tagsetItemsCurrent;

        int valore = 0;
        int start = offset.start, end = random.nextInt(2, 10);
        while (end < offset.end) {
            System.out.println(start + " - " + end);
            layer = layers.get(random.nextInt(layers.size()));
            annotation = new Annotation();
            annotation.setResource(resource);
            annotation.setLayer(layer);
            annotation.setStart(start);
            annotation.setEnd(end);
            annotation.setUser(user);
            domainManager.create(annotation);
            for (Feature feature : features.get(layer)) {
                annotationFeature = new AnnotationFeature();
                annotationFeature.setAnnotation(annotation);
                annotationFeature.setFeature(feature);
                if (feature.getTagset() != null) {
                    tagsetItemsCurrent = tagsetItems.get(feature.getTagset());
                    if (!tagsetItemsCurrent.isEmpty()) {
                        annotationFeature.setValue(tagsetItemsCurrent.get(random.nextInt(tagsetItemsCurrent.size())).getName());
                    } else {
                        annotationFeature.setValue("valore " + valore++);
                    }
                } else {
                    annotationFeature.setValue("valore " + valore++);
                }
                domainManager.create(annotationFeature);
            }
            start = start + random.nextInt(4, 8);
            end = start + random.nextInt(2, 10);
        }
    }

}
