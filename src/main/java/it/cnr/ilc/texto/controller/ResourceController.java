package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.manager.AnnotationManager;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.FolderManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.manager.ResourceManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("resource")
public class ResourceController extends EntityController<Resource> {

    @Autowired
    private ResourceManager resourceManager;
    @Autowired
    private FolderManager folderManager;
    @Autowired
    private AnnotationManager annotationManager;

    @Override
    protected Class<Resource> entityClass() {
        return Resource.class;
    }

    @Override
    protected EntityManager<Resource> entityManager() {
        return resourceManager;
    }

    @Override
    protected void checkAccess(Resource resource, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        super.checkAccess(resource, action);
        Folder parent = resource.getParent();
        if (parent == null) {
            accessManager.checkAccess(parent, Action.WRITE);
        }
    }

    @Override
    protected void preCreate(Resource resource) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        if (resource.getName() != null && resource.getName().isBlank()) {
            throw new ManagerException("blank name");
        }
        if (resourceManager.exists(resource.getParent(), resource.getName())) {
            throw new ManagerException("name exists");
        }
    }

    @Override
    protected void preUpdateComplete(Resource previous, Resource resource) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        if (resource.getName() != null && resource.getName().isBlank()) {
            throw new ManagerException("blank name");
        }
        if (resourceManager.exists(resource.getParent(), resource.getName())) {
            throw new ManagerException("name exists");
        }
    }

    @Override
    protected void preUpdatePartial(Resource previous, Resource entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        accessManager.checkAccess(previous, Action.WRITE);
        if (entity.getName() != null && entity.getName().isBlank()) {
            throw new ManagerException("blank name");
        }
        if (entity.getParent() != null) {
            if (folderManager.exists(entity.getParent(), entity.getName() == null ? previous.getName() : entity.getName())) {
                throw new ManagerException("name exsists");
            }
        }
        if (entity.getName() != null && resourceManager.exists(previous.getParent(), entity.getName())) {
            throw new ManagerException("name exsists");
        }
    }

    @PostMapping("{id}/upload")
    public void multipart(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) throws Exception {
        logManager.setMessage("upload " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.WRITE);
        resourceManager.upload(resource, file.getInputStream());
    }

    @GetMapping("{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("download " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.READ);
        InputStreamResource inputStreamResource = new InputStreamResource(resourceManager.download(resource));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(inputStreamResource);
    }

    @PostMapping("{id}/text")
    public String text(@PathVariable("id") Long id, @RequestBody Offset offset) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get text of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.READ);
        logManager.appendMessage(offset.toString());
        return resourceManager.getText(resource, offset);
    }

    @PostMapping("{id}/rows")
    public Map<String, Object> rows(@PathVariable("id") Long id, @RequestBody Offset offset) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get rows of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        logManager.appendMessage(offset.toString());
        accessManager.checkAccess(resource, Action.READ);
        return resourceManager.getRows(resource, offset);
    }

    @PostMapping("{id}/annotations")
    public List<Map<String, Object>> annotations(@PathVariable("id") Long id, @RequestBody Offset offset) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get annotations of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        logManager.appendMessage(offset.toString());
        accessManager.checkAccess(resource, Action.READ);
        Offset abbsolute = resourceManager.getAbsoluteOffset(resource, offset);
        return annotationManager.getAnnotations(resource, abbsolute);
    }

}
