package it.cnr.ilc.texto;

import it.cnr.ilc.texto.controller.Controller;
import it.cnr.ilc.texto.manager.AccessManager;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import it.cnr.ilc.texto.manager.LogManager;
import it.cnr.ilc.texto.manager.MonitorManager;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@SpringBootApplication
@RestController
@RequestMapping("")
public class Application implements WebMvcConfigurer {

    public static void main(String[] args) throws IOException {
        try (ConfigurableApplicationContext context = SpringApplication.run(Application.class, args)) {
            int port = context.getEnvironment().getRequiredProperty("server.port-shutdown", Number.class).intValue();
            try (ServerSocket server = new ServerSocket(port)) {
                boolean shutdown = false;
                while (!shutdown) {
                    try (Socket socket = server.accept()) {
                        if (socket.getInetAddress().isAnyLocalAddress() || socket.getInetAddress().isLoopbackAddress()) {
                            shutdown = true;
                        }
                    }
                }
            }
            context.getBean(AccessManager.class).close();
            context.getBean(DatabaseManager.class).close();
        }
    }

    @Autowired
    protected Environment environment;
    @Autowired
    private DatabaseManager databaseManager;
    @Autowired
    private DomainManager domainManager;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private MonitorManager monitorManager;
    @Autowired
    private LogManager logManager;

    @GetMapping("")
    public String index() {
        String name = environment.getProperty("application.name");
        String version = environment.getProperty("application.version");
        return name + " ver. " + version + " is running!";
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                .allowedOrigins("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> paths = getRequestPaths();
        registry.addInterceptor(new DatabaseInterceptor(databaseManager, domainManager))
                .addPathPatterns(paths);
        registry.addInterceptor(new ApplicationInterceptor(accessManager, monitorManager, logManager))
                .addPathPatterns(paths)
                .excludePathPatterns("/authentication/login");
    }

    private List<String> getRequestPaths() {
        final Set<String> paths = new HashSet<>();
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(Controller.class.getPackageName()));
        reflections.getSubTypesOf(Controller.class).stream().forEach(clazz -> {
            RequestMapping requestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String path = "/" + requestMapping.value()[0];
                for (Method method : clazz.getMethods()) {
                    GetMapping getMapping = method.getAnnotation(GetMapping.class);
                    if (getMapping != null) {
                        paths.add(path + "/" + getMapping.value()[0]);
                    }
                    PostMapping postMapping = method.getAnnotation(PostMapping.class);
                    if (postMapping != null) {
                        paths.add(path + "/" + postMapping.value()[0]);
                    }
                    DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                    if (deleteMapping != null) {
                        paths.add(path + "/" + deleteMapping.value()[0]);
                    }
                }
            }
        });
        return paths.stream().collect(Collectors.toList());
    }

}
