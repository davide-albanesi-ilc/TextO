package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.AccessManager.AccessImplementation;
import it.cnr.ilc.texto.manager.AccessManager.Session;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author oakgen
 */
public abstract class ExternalAccessImplementation extends AccessImplementation {

    private final Map<Thread, Session> threads = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private long timeout;

    @Override
    protected void init() throws Exception {
        timeout = Long.parseLong(environment.getProperty("access.session-timeout", "1800")) * 1000;
    }

    @Override
    protected void startRequest(HttpServletRequest request) throws Exception {
        String token = request.getHeader("authorization");
        if (token == null) {
            throw new AuthorizationException("authorization token missing");
        }
        token = retrieveToken(token);
        Session session = sessions.get(token);
        if (session != null) {
            Timer timer = session.getTimer();
            timer.cancel();
            timer.purge();
        } else {
            session = new Session();
            session.setUser(retrieveUser(token));
            session.setToken(token);
            Timer timer = new Timer();
            timer.schedule(new CancelTask(session.getToken()), timeout);
            session.setTimer(timer);
            session.setFirstActionTime(LocalDateTime.now());
            session.setLastActionTime(LocalDateTime.now());
            session.setExpiredTime(LocalDateTime.now().plusSeconds(timeout / 1000));
            sessions.put(session.getToken(), session);
        }
        threads.put(Thread.currentThread(), session);
    }

    protected abstract String retrieveToken(String token) throws Exception;

    protected abstract User retrieveUser(String token) throws Exception;

    @Override
    protected User getUser() {
        return threads.get(Thread.currentThread()).getUser();
    }

    @Override
    protected void endRequest() throws Exception {
        Session session = threads.remove(Thread.currentThread());
        if (session != null) {
            Timer timer = session.getTimer();
            timer.cancel();
            timer.purge();
            timer = new Timer();
            timer.schedule(new CancelTask(session.getToken()), timeout);
            session.setTimer(timer);
            session.setLastActionTime(LocalDateTime.now());
            session.setExpiredTime(LocalDateTime.now().plusSeconds(timeout / 1000));
            sessions.put(session.getToken(), session);
        }
    }

    @Override
    protected String startSession(User user) {
        throw new UnsupportedOperationException("session not supported");
    }

    @Override
    protected Session getSession() {
        return threads.get(Thread.currentThread());
    }

    @Override
    protected void endSession() {
        throw new UnsupportedOperationException("session not supported");
    }

    @Override
    protected Collection<Session> getSessions() {
        return sessions.values();
    }

    private class CancelTask extends TimerTask {

        private final String token;

        private CancelTask(String token) {
            this.token = token;
        }

        @Override
        public void run() {
            Session session = sessions.remove(token);
            Timer timer = session.getTimer();
            timer.cancel();
            timer.purge();
        }

    }
}
