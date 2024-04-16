package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.AccessManager.AccessImplementation;
import it.cnr.ilc.texto.manager.AccessManager.Session;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author oakgen
 */
public class OpenAccessImplementation extends AccessImplementation {

    private User user;

    @Override
    protected void init() throws Exception {
        user = domainManager.load(User.class, 4l);
    }

    protected User retrieveUser() throws SQLException, ReflectiveOperationException, ManagerException {
        return null;
    }

    @Override
    protected void startRequest(HttpServletRequest request) throws Exception {
    }

    @Override
    protected User getUser() {
        return user;
    }

    @Override
    protected void endRequest() throws Exception {
    }

    @Override
    protected String startSession(User user) throws Exception {
        throw new UnsupportedOperationException("session not supported");
    }

    @Override
    protected Session getSession() {
        Session session = new Session();
        session.setUser(getUser());
        return session;
    }

    @Override
    protected void endSession() throws Exception {
        throw new UnsupportedOperationException("session not supported");
    }

    @Override
    protected List<Session> getSessions() {
        return Collections.EMPTY_LIST;
    }

}
