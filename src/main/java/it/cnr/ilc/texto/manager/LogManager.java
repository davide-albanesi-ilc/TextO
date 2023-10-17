package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.controller.Controller;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.manager.AccessManager.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class LogManager extends Manager {

    private final Logger logger = LoggerFactory.getLogger(Controller.class);

    @Autowired
    private AccessManager accessManager;

    private final Map<Thread, StringBuilder> messages = new ConcurrentHashMap<>();

    public LogManager setMessage(String message) {
        messages.put(Thread.currentThread(), new StringBuilder(message));
        return this;
    }

    public LogManager appendMessage(String message) {
        StringBuilder builder = messages.get(Thread.currentThread());
        if (builder == null) {
            builder = new StringBuilder(message);
            messages.put(Thread.currentThread(), builder);
        }
        builder.append(" ").append(message);
        return this;
    }

    public LogManager appendMessage(Class<? extends Entity> clazz) {
        return appendMessage(clazz.getSimpleName());
    }

    public void logMessage(Exception ex) {
        StringBuilder message = messages.remove(Thread.currentThread());
        if (message != null) {
            Session session = accessManager.getSession();
            StringBuilder builder = new StringBuilder();
            builder.append("(").append(session.getUser().getUsername()).append(") ")
                    .append("[").append(session.getToken().substring(0, Math.min(32, session.getToken().length()))).append("] ")
                    .append(message);
            if (ex == null) {
                logger.info(builder.toString());
            } else {
                logger.error(builder.toString(), ex);
            }
        }
    }

}
