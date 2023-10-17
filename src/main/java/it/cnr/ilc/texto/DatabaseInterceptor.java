package it.cnr.ilc.texto;

import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 *
 * @author oakgen
 */
public class DatabaseInterceptor implements HandlerInterceptor {

    private final DatabaseManager databaseManager;
    private final DomainManager domainManager;

    DatabaseInterceptor(DatabaseManager databaseManager, DomainManager domainManager) {
        this.databaseManager = databaseManager;
        this.domainManager = domainManager;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        domainManager.freeCache();
        if (ex == null) {
            databaseManager.commitAndReleaseConnection();
        } else {
            databaseManager.rollbackAndReleaseConnection();
        }
    }

}
