package it.cnr.ilc.texto.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.manager.AccessManager.Session;
import it.cnr.ilc.texto.manager.AnalysisManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
                                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getKey().contains("password") ? "******" : e.getValue().toString())));
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

    @GetMapping("session/list")
    public Collection<Session> sessions() throws ForbiddenException {
        accessManager.checkAccess(System.class, Action.READ);
        logManager.setMessage("get sessions");
        return accessManager.getSessions();
    }

    @GetMapping("logger/{logger}")
    public String getLogger(@PathVariable("logger") String name) throws ForbiddenException, ManagerException {
        accessManager.checkAccess(System.class, Action.READ);
        logManager.setMessage("get logger " + name);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.exists(name);
        if (logger == null) {
            throw new ManagerException("logger not found");
        }
        return logger.getName() + " " + logger.getLevel();
    }

    @PostMapping("logger/{logger}")
    public String setLogger(@PathVariable("logger") String loggerName, @RequestParam(name = "level") String levelName) throws ForbiddenException, ManagerException {
        accessManager.checkAccess(System.class, Action.WRITE);
        logManager.setMessage("set logger " + loggerName + " to level " + levelName.toUpperCase());
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.exists(loggerName);
        if (logger == null) {
            throw new ManagerException("logger not found");
        }
        Level level = Level.toLevel(levelName, null);
        if (level == null) {
            throw new ManagerException("level not found");
        }
        logger.setLevel(level);
        return logger.getName() + " " + logger.getLevel();
    }

    @GetMapping("init/analysis")
    public void initAnalysis() throws SQLException, ReflectiveOperationException, ManagerException, ForbiddenException {
        accessManager.checkAccess(System.class, Action.WRITE);
        logManager.setMessage("init analysis layers");
        analysisManager.initLayers();
    }

}
