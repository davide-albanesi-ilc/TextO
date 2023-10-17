package it.cnr.ilc.texto.manager.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import java.util.Base64;
import java.util.Map;
import org.springframework.core.env.Environment;

/**
 *
 * @author oakgen
 */
public abstract class JWTExternAlccessImplementation extends ExternalAccessImplementation {

    private final ObjectMapper mapper = new ObjectMapper();

    public JWTExternAlccessImplementation(Environment environment, DatabaseManager databaseManager, DomainManager entityManager) {
        super(environment, databaseManager, entityManager);
    }

    @Override
    protected String retrieveToken(String token) throws Exception {
        if (!token.toLowerCase().startsWith("bearer")) {
            throw new AuthorizationException("invalid authorization parameter");
        }
        String[] chunks = token.substring(6).trim().split("\\.");
        return chunks[1];
    }

    @Override
    protected synchronized User retrieveUser(String token) throws Exception {
        token = new String(Base64.getUrlDecoder().decode(token));
        Map<String, Object> payload = mapper.readValue(token, Map.class);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(DomainManager.quote(User.class))
                .append(" where status = 1 and username = '").append(retrieveUsername(payload)).append("'");
        User user = domainManager.loadUnique(User.class, sql.toString());
        if (user == null) {
            user = retrieveUser(payload);
            beforeCreationUser(user);
            domainManager.create(user);
            afterCreationUser(user);
            databaseManager.getConnection().commit();
        }
        return user;
    }

    protected abstract String retrieveUsername(Map<String, Object> payload) throws Exception;

    protected abstract User retrieveUser(Map<String, Object> payload) throws Exception;

    protected void beforeCreationUser(User user) throws Exception {
    }

    protected void afterCreationUser(User user) throws Exception {
    }
}
