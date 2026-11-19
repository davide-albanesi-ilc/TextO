package it.cnr.ilc.texto;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.cnr.ilc.texto.controller.Controller;
import it.cnr.ilc.texto.manager.AccessManager;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import it.cnr.ilc.texto.manager.LogManager;
import it.cnr.ilc.texto.manager.MonitorManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Configuration
@SpringBootApplication
@RestControllerAdvice
@RestController
@RequestMapping("/")
public class Application implements WebMvcConfigurer, Filter {

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
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
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
                String mapping, path = "/" + requestMapping.value()[0];
                for (Method method : clazz.getMethods()) {
                    GetMapping getMapping = method.getAnnotation(GetMapping.class);
                    if (getMapping != null) {
                        mapping = getMapping.value()[0];
                        paths.add(path + (mapping.isEmpty() ? "" : ("/" + mapping)));
                    }
                    PostMapping postMapping = method.getAnnotation(PostMapping.class);
                    if (postMapping != null) {
                        mapping = postMapping.value()[0];
                        paths.add(path + (mapping.isEmpty() ? "" : ("/" + mapping)));
                    }
                    PutMapping putMapping = method.getAnnotation(PutMapping.class);
                    if (putMapping != null) {
                        mapping = putMapping.value()[0];
                        paths.add(path + (mapping.isEmpty() ? "" : ("/" + mapping)));
                    }
                    DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                    if (deleteMapping != null) {
                        mapping = deleteMapping.value()[0];
                        paths.add(path + (mapping.isEmpty() ? "" : ("/" + mapping)));
                    }
                }
            }
        });
        return paths.stream().collect(Collectors.toList());
    }

    private static final int WRAP_LIMIT = 8192;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Logger logger = (Logger) LoggerFactory.getLogger(ApplicationInterceptor.class);
        if (logger.getEffectiveLevel() != Level.OFF) {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper((HttpServletRequest) request, WRAP_LIMIT);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);
            chain.doFilter(wrappedRequest, wrappedResponse);
            logger.debug(getLog(wrappedRequest));
            logger.debug(getLog(wrappedResponse, wrappedRequest.getRequestId()));
            wrappedResponse.copyBodyToResponse();
        } else {
            chain.doFilter(request, response);
        }
    }

    private String getLog(ContentCachingRequestWrapper request) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(request.getRequestId()).append(") ")
                .append("[").append(request.getRemoteAddr()).append(":").append(request.getRemotePort()).append("] ")
                .append(request.getMethod()).append(" ")
                .append("'").append(request.getServletPath())
                .append(request.getQueryString() == null ? "" : "?" + URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8)).append("'");
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, 0, Math.min(content.length, WRAP_LIMIT)).replaceAll("\\s+", " ");
            builder.append(" ").append(request.getContentType()).append(" '").append(body).append("'");
        }
        return builder.toString();
    }

    private String getLog(ContentCachingResponseWrapper response, String requestId) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(requestId).append(") ")
                .append(response.getStatus());
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, 0, Math.min(content.length, WRAP_LIMIT)).replaceAll("\\s+", " ");
            builder.append(" ").append(response.getContentType()).append(" '").append(body).append("'");
        }
        return builder.toString();
    }
}
