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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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
    public List<E> list(@RequestParam(required = false, name = "where") String where) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get").appendMessage(entityClass()).appendMessage("list");
        checkAccess(Action.READ);
        if (where != null) {
            logManager.appendMessage("where").appendMessage(where);
            StringBuilder builder = new StringBuilder();
            builder.append("select * from ").append(quote(entityClass()))
                    .append(" where ").append(where);
            return entityManager().load(builder.toString());
        } else {
            return entityManager().load();
        }
    }

    @GetMapping("{id}")
    public E get(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("get").appendMessage(entityClass());
        E entity = entityManager().load(id);
        if (entity == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("entity not found");
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
        return entity;
    }

    @PostMapping("create")
    public E create(@RequestBody E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("create").appendMessage(entityClass().getSimpleName()).appendMessage(entityManager().getLog(entity));
        setUser(entity);
        checkAccess(entity, Action.CREATE);
        entityManager().create(entity);
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
        entityManager().update(entity);
        return entity;
    }

    @PostMapping("{id}/update")
    public E update(@PathVariable("id") Long id, @RequestBody Map<String, Object> values) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("update").appendMessage(entityClass());
        E previous = entityManager().load(id);
        if (previous == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(previous));
        checkAccess(previous, Action.WRITE);
        E entity = entityManager().update(id, values);
        return entity;
    }

    @DeleteMapping("remove")
    public Map<String, Object> remove(@RequestBody E entity) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("remove").appendMessage(entityClass());
        E previous = entityManager().load(entity.getId());
        if (previous == null) {
            logManager.appendMessage("" + entity.getId());
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(previous));
        setUser(entity);
        checkAccess(previous, Action.REMOVE);
        entityManager().remove(entity);
        return domainManager.toMap(entity);
    }

    @DeleteMapping("{id}/remove")
    public Map<String, Object> remove(@PathVariable("id") Long id) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        logManager.setMessage("remove").appendMessage(entityClass());
        E previous = entityManager().load(id);
        if (previous == null) {
            logManager.appendMessage("" + id);
            throw new ManagerException("not found");
        }
        logManager.appendMessage(entityManager().getLog(previous));
        checkAccess(previous, Action.REMOVE);
        E entity = entityManager().remove(id);
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
        E presious = entities.getLast();
        logManager.appendMessage(entityManager().getLog(entity));
        setUser(entity);
        checkAccess(presious, Action.REMOVE);
        entityManager().restore(entity);
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
        E presious = entities.getLast();
        logManager.appendMessage(entityManager().getLog(presious));
        checkAccess(presious, Action.REMOVE);
        E entity = entityManager().restore(id);
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
