package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.controller.Controller;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.manager.AccessManager.Session;
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

    private final ThreadLocal<StringBuilder> messages = new ThreadLocal<>();
    private final ThreadLocal<Exception> exceptions = new ThreadLocal<>();

    public LogManager setMessage(String text) {
        messages.set(new StringBuilder(text));
        return this;
    }

    public LogManager appendMessage(String text) {
        StringBuilder builder = messages.get();
        if (builder == null) {
            messages.set(new StringBuilder(text));
        } else {
            builder.append(" ").append(text);
        }
        return this;
    }

    public LogManager appendMessage(Class<? extends Entity> clazz) {
        return appendMessage(clazz.getSimpleName());
    }

    public boolean isMessageEmty() {
        return messages.get() == null;
    }
    public void setException(Exception e) {
        exceptions.set(e);
    }

    public void logMessage() {
        StringBuilder builder = messages.get();
        Exception exception = exceptions.get();
        messages.remove();
        exceptions.remove();
        if (builder != null) {
            Session session = accessManager.getSession();
            String line = "(" + session.getUser().getUsername() + ") [" + session.getToken().substring(0, Math.min(32, session.getToken().length())) + "] " + builder;
            if (exception == null) {
                logger.info(line);
            } else {
                logger.error(line, exception);
            }
        }
    }

}
