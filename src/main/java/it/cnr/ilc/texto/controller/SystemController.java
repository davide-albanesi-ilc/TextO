package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.manager.AccessManager.Session;
import it.cnr.ilc.texto.manager.AnalysisManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
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

    @Autowired
    private AnalysisManager analysisManager;

    @GetMapping("info")
    public Map<String, Object> info() {
        logManager.setMessage("get system info");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application.name", environment.getProperty("application.name"));
        info.put("application.version", environment.getProperty("application.version"));
        info.put("server.time", LocalDateTime.now());
        return info;
    }

    @GetMapping("environment")
    public Map<String, Map<String, String>> environment() throws ForbiddenException {
        accessManager.checkAccess(System.class, Action.READ);
        logManager.setMessage("get environment");
        Map<String, Map<String, String>> map = new HashMap();
        for (PropertySource propertySource : ((AbstractEnvironment) environment).getPropertySources()) {
            if (propertySource instanceof MapPropertySource mapPropertySource) {
                map.put(mapPropertySource.getName(),
                        mapPropertySource.getSource().entrySet().stream()
                                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())));
            }
        }
        return map;
    }

    @GetMapping("session")
    public Session session() throws ForbiddenException {
        accessManager.checkAccess(System.class, Action.READ);
        logManager.setMessage("get session");
        return accessManager.getSession();
    }

    @GetMapping("sessions")
    public Collection<Session> sessions() throws ForbiddenException {
        accessManager.checkAccess(System.class, Action.READ);
        logManager.setMessage("get sessions");
        return accessManager.getSessions();
    }

    @GetMapping("init/analysis")
    public void initAnalysis() throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException, IOException, URISyntaxException, InterruptedException {
        accessManager.checkAccess(System.class, Action.WRITE);
        logManager.setMessage("init analysis layers");
        analysisManager.initLayers();
    }
}
