package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.manager.AccessManager.Session;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("system")
public class SystemController extends Controller {

    @GetMapping("info")
    public Map<String, Object> info() {
        logManager.setMessage("get system info");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application.name", environment.getProperty("application.name"));
        info.put("application.version", environment.getProperty("application.version"));
        info.put("java.version", environment.getProperty("java.version"));
        info.put("server.time", LocalDateTime.now());
        return info;
    }

    @GetMapping("session")
    public Session session() throws ForbiddenException {
        logManager.setMessage("get session");
        accessManager.checkAccess(System.class, Action.READ);
        return accessManager.getSession();
    }

    @GetMapping("sessions")
    public Collection<Session> sessions() throws ForbiddenException {
        logManager.setMessage("get sessions");
        accessManager.checkAccess(System.class, Action.READ);
        return accessManager.getSessions();
    }

}
