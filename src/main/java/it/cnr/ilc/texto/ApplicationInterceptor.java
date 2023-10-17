package it.cnr.ilc.texto;

import it.cnr.ilc.texto.manager.AccessManager;
import it.cnr.ilc.texto.manager.LogManager;
import it.cnr.ilc.texto.manager.MonitorManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 *
 * @author oakgen
 */
public class ApplicationInterceptor implements HandlerInterceptor {

    private final AccessManager accessManager;
    private final MonitorManager monitorManager;
    private final LogManager logManager;

    public ApplicationInterceptor(AccessManager accessManager, MonitorManager monitorManager, LogManager logManager) {
        this.accessManager = accessManager;
        this.monitorManager = monitorManager;
        this.logManager = logManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!request.getMethod().equals(RequestMethod.OPTIONS.name())) {
            accessManager.startRequest(request);
            monitorManager.startRequest(request);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (!request.getMethod().equals(RequestMethod.OPTIONS.name())) {
            logManager.logMessage(ex);
            monitorManager.endRequest();
        }
        accessManager.endRequest();
    }
}
