package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Entity;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class EntityManager<E extends Entity> extends Manager {

    @Autowired
    protected DatabaseManager databaseManager;

    @Autowired
    protected DomainManager domainManager;

    protected abstract Class<E> entityClass();

    public abstract String getLog(E entity) throws SQLException, ReflectiveOperationException, ManagerException;

    public List<E> load() throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.load(entityClass());
    }

    public E load(Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.load(entityClass(), id);
    }

    public List<E> load(String sql) throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.load(entityClass(), sql);
    }

    public E loadUnique(String sql) throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.loadUnique(entityClass(), sql);
    }

    public E create() throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.create(entityClass());
    }

    public void create(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        domainManager.create(entity);
    }

    public void update(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        domainManager.update(entity);
    }

    public void update(E previous, E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        domainManager.update(previous, entity);
    }

    public E update(Long id, E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.update(entityClass(), id, entity);
    }

    public void remove(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        domainManager.remove(entity);
    }

    public E remove(Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.remove(entityClass(), id);
    }

    public List<E> history(Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.history(entityClass(), id);
    }

    public void restore(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        domainManager.restore(entity);
    }

    public E restore(Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.restore(entityClass(), id);
    }

}
