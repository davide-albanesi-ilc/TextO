package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.AnalysisManager;
import it.cnr.ilc.texto.manager.AnnotationManager;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.manager.ResourceManager;
import it.cnr.ilc.texto.manager.RowManager;
import it.cnr.ilc.texto.manager.SectionManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private SectionManager sectionManager;
    @Autowired
    private RowManager rowManager;
    @Autowired
    private AnnotationManager annotationManager;
    @Autowired
    private AnalysisManager analysisManager;

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
        if (parent != null) {
            accessManager.checkAccess(parent, Action.WRITE);
        }
    }

    @GetMapping("uploaders")
    public Set<String> text() throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get uploaders");
        return resourceManager.getUploaders();
    }

    @PostMapping("{id}/upload")
    public void multipart(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file, @RequestParam Map<String, String> parameters) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException, IOException {
        logManager.setMessage("upload " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.WRITE);
        resourceManager.upload(resource, new String(file.getBytes()), parameters);
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

    @GetMapping("{id}/text")
    public String text(@PathVariable("id") Long id, @RequestParam Map<String, String> params) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get text of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.READ);
        Offset offset = Offset.fromMap(params);
        logManager.appendMessage(offset.toString());
        return resourceManager.getText(resource, offset);
    }

    @GetMapping("{id}/sections")
    public List<Section> sections(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get sections of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        accessManager.checkAccess(resource, Action.READ);
        return sectionManager.load(resource);
    }

    @GetMapping("{id}/rows")
    public List<Row> rows(@PathVariable("id") Long id, @RequestParam Map<String, String> params) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get rows of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        Offset offset = Offset.fromMap(params);
        logManager.appendMessage(offset.toString());
        accessManager.checkAccess(resource, Action.READ);
        return rowManager.load(resource, offset);
    }

    @GetMapping("{id}/rowCount")
    public Integer rowCount(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get row count of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.READ);
        return rowManager.rowCount(resource);
    }

    @GetMapping("{id}/annotations")
    public List<Annotation> annotations(@PathVariable("id") Long id, @RequestParam Map<String, String> params) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get annotations of " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        Offset offset = Offset.fromMap(params);
        logManager.appendMessage(offset.toString());
        accessManager.checkAccess(resource, Action.READ);
        return annotationManager.load(resource, offset);
    }

    @GetMapping("{id}/analize")
    public void analysis(@PathVariable("id") Long id) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("crete analysis for " + entityClass().getSimpleName());
        Resource resource = resourceManager.load(id);
        if (resource == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(resourceManager.getLog(resource));
        accessManager.checkAccess(resource, Action.WRITE);
        User user = accessManager.getUser();
        analysisManager.analize(resource, user);
    }

}
