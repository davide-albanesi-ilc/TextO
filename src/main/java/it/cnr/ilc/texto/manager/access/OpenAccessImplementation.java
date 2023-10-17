package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.AccessManager.AccessImplementation;
import it.cnr.ilc.texto.manager.AccessManager.Session;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.springframework.core.env.Environment;

/**
 *
 * @author oakgen
 */
public class OpenAccessImplementation extends AccessImplementation {

    private final User user;

    public OpenAccessImplementation(Environment environment, DatabaseManager databaseManager, DomainManager domainManager) throws SQLException, ReflectiveOperationException, ManagerException {
        super(environment, databaseManager, domainManager);
        user = retrieveUser();
    }

    protected User retrieveUser() throws SQLException, ReflectiveOperationException, ManagerException {
        return null;
    }

    @Override
    public void startRequest(HttpServletRequest request) throws Exception {
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void endRequest() throws Exception {
    }

    @Override
    public String startSession(User user) throws Exception {
        throw new UnsupportedOperationException("session not supported");
    }

    @Override
    public Session getSession() {
        Session session = new Session();
        session.setUser(getUser());
        return session;
    }

    @Override
    public void endSession() throws Exception {
        throw new UnsupportedOperationException("session not supported");
    }

    @Override
    public List<Session> getSessions() {
        return Collections.EMPTY_LIST;
    }

}
