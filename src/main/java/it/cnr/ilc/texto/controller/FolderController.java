package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.FolderManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.manager.ResourceManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("folder")
public class FolderController extends EntityController<Folder> {

    @Autowired
    private FolderManager folderManager;
    @Autowired
    private ResourceManager resourceManager;

    @Override
    protected Class<Folder> entityClass() {
        return Folder.class;
    }

    @Override
    protected EntityManager<Folder> entityManager() {
        return folderManager;
    }

    @Override
    protected void checkAccess(Folder folder, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        super.checkAccess(folder, action);
        Folder parent = folder.getParent();
        if (parent != null) {
            accessManager.checkAccess(parent, Action.WRITE);
        }
    }

    @Override
    protected void preCreate(Folder folder) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        if (folder.getName() != null && resourceManager.exists(folder.getParent(), folder.getName())) {
            throw new ManagerException("name exists");
        }
    }

    @Override
    protected void preUpdateComplete(Folder previous, Folder folder) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        if (previous.getParent() == null) {
            throw new ManagerException("unable to update home");
        }
        if (folder.getName() != null && resourceManager.exists(folder.getParent(), folder.getName())) {
            throw new ManagerException("name exists");
        }
    }

    @Override
    protected void preUpdatePartial(Folder previous, Folder entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        if (previous.getParent() == null) {
            throw new ManagerException("unable to update home");
        }
        if (entity.getParent() != null) {
            if (resourceManager.exists(entity.getParent(), entity.getName() == null ? previous.getName() : entity.getName())) {
                throw new ManagerException("name exists");
            }
        }
        if (entity.getName() != null && resourceManager.exists(previous.getParent(), entity.getName())) {
            throw new ManagerException("name exists");
        }
    }

    @Override
    protected void preRemove(Folder folder) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        if (folder.getParent() == null) {
            throw new ManagerException("unable to remove home");
        }
    }

    @GetMapping("{id}/path")
    public String path(@PathVariable("id") Long id) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get path of").appendMessage(entityClass().getSimpleName());
        Folder folder = folderManager.load(id);
        if (folder == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(folderManager.getLog(folder));
        accessManager.checkAccess(folder, Action.READ);
        return folderManager.getPath(folder);
    }

    @GetMapping("{id}/list")
    public List<Map<String, Object>> list(@PathVariable("id") Long id) throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        logManager.setMessage("get list of").appendMessage(entityClass().getSimpleName());
        Folder folder = folderManager.load(id);
        if (folder == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(folderManager.getLog(folder));
        accessManager.checkAccess(folder, Action.READ);
        return folderManager.list(folder);
    }

}
