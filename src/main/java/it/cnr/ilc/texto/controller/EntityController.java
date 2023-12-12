package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Userable;
import it.cnr.ilc.texto.manager.DomainManager;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 *
 * @author oakgen
 * @param <E>
 */
public abstract class EntityController<E extends Entity> extends Controller {

    @Autowired
    protected DomainManager domainManager;

    protected abstract EntityManager<E> entityManager();

    protected abstract Class<E> entityClass();

    @GetMapping("list")
    public List<E> list() throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get").appendMessage(entityClass()).appendMessage("list");
        checkAccess(Action.READ);
        return entityManager().load();
    }

    @PostMapping("list")
    public List<E> list(@RequestBody String where) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get").appendMessage(entityClass()).appendMessage("filtered list");
        checkAccess(Action.READ);
        StringBuilder builder = new StringBuilder();
        builder.append("select * from ").append(quote(entityClass()))
                .append(" where status = 1 and (").append(where).append(")");
        return entityManager().load(builder.toString());
    }

    @GetMapping("{id}")
    public E get(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get").appendMessage(entityClass());
        E entity = entityManager().load(id);
        if (entity == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(entity));
        checkAccess(entity, Action.READ);
        return entity;
    }

    @GetMapping("create")
    public E create() throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("create empty").appendMessage(entityClass());
        checkAccess(Action.CREATE);
        E entity = entityManager().create();
        postCreate(entity);
        return entity;
    }

    @PostMapping("create")
    public E create(@RequestBody E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("create").appendMessage(entityClass().getSimpleName()).appendMessage(entityManager().getLog(entity));
        setUser(entity);
        checkAccess(entity, Action.CREATE);
        preCreate(entity);
        entityManager().create(entity);
        postCreate(entity);
        return entity;
    }

    @PostMapping("update")
    public E update(@RequestBody E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("update").appendMessage(entityClass());
        E previous = entityManager().load(entity.getId());
        if (previous == null) {
            logManager.appendMessage("" + entity.getId());
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(entity));
        setUser(entity);
        checkAccess(previous, Action.WRITE);
        preUpdateComplete(previous, entity);
        entityManager().update(entity);
        postUpdate(entity);
        return entity;
    }

    @PostMapping("{id}/update")
    public E update(@PathVariable("id") Long id, @RequestBody E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("update").appendMessage(entityClass());
        E previous = entityManager().load(id);
        if (previous == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(previous));
        checkAccess(previous, Action.WRITE);
        preUpdatePartial(previous, entity);
        entityManager().update(id, entity);
        postUpdate(entity);
        return entity;
    }

    @PostMapping("remove")
    public Map<String, Object> remove(@RequestBody E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("remove").appendMessage(entityClass());
        E previous = entityManager().load(entity.getId());
        if (previous == null) {
            logManager.appendMessage("" + entity.getId());
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(previous));
        setUser(entity);
        checkAccess(entity, Action.REMOVE);
        preRemove(entity);
        entityManager().remove(entity);
        postRemove(entity);
        return domainManager.toMap(entity);
    }

    @GetMapping("{id}/remove")
    public Map<String, Object> remove(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("remove").appendMessage(entityClass());
        E entity = entityManager().load(id);
        if (entity == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(entity));
        checkAccess(entity, Action.REMOVE);
        preRemove(entity);
        entityManager().remove(id);
        postRemove(entity);
        return domainManager.toMap(entity);
    }

    @PostMapping("restore")
    public E restore(@RequestBody E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("restore").appendMessage(entityClass());
        List<E> entities = entityManager().history(entity.getId());
        if (entities.isEmpty()) {
            logManager.appendMessage("" + entity.getId());
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(entity));
        setUser(entity);
        checkAccess(entity, Action.WRITE);
        preRestore(entity);
        entityManager().restore(entity);
        postRestore(entity);
        return entity;
    }

    @GetMapping("{id}/restore")
    public E restore(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("restore").appendMessage(entityClass());
        List<E> entities = entityManager().history(id);
        if (entities.isEmpty()) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        E entity = entities.get(0);
        logManager.appendMessage(entityManager().getLog(entities.get(0)));
        checkAccess(entity, Action.WRITE);
        preRestore(entity);
        entityManager().restore(entity);
        postRestore(entity);
        return entity;
    }

    @GetMapping("{id}/history")
    public List<Map<String, Object>> history(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get history").appendMessage(entityClass());
        checkAccess(Action.READ);
        List<E> entities = entityManager().history(id);
        if (entities.isEmpty()) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(entities.get(0)));
        checkAccess(entities.get(0), Action.READ);
        return domainManager.toMap(entities);
    }

    protected void preCreate(E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void postCreate(E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void preUpdateComplete(E previous, E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void preUpdatePartial(E previous, E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void postUpdate(E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void preRemove(E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void postRemove(E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void preRestore(E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    protected void postRestore(E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
    }

    private void setUser(E entity) {
        if (entity instanceof Userable userable && userable.getUser() == null) {
            userable.setUser(accessManager.getUser());
        }
    }

    protected void checkAccess(Action action) throws ForbiddenException, ManagerException {
        accessManager.checkAccess(entityClass(), action);
    }

    protected void checkAccess(E entity, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        accessManager.checkAccess(entity, action);
    }

}
