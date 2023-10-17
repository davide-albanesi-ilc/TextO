package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Role;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;

/**
 *
 * @author oakgen
 */
public class MaiaKeycloakAccessImplementation extends JWTExternAlccessImplementation {

    private final Map<String, String> matches = new HashMap<>();

    public MaiaKeycloakAccessImplementation(Environment environment, DatabaseManager databaseManager, DomainManager entityManager) {
        super(environment, databaseManager, entityManager);
        matches.put("AMMINISTRATORE", "Administrator");
    }

    @Override
    protected String retrieveUsername(Map<String, Object> payload) throws Exception {
        return (String) payload.get("preferred_username");
    }

    @Override
    protected User retrieveUser(Map<String, Object> payload) throws Exception {
        User user = new User();
        user.setUsername((String) payload.get("username"));
        user.setName((String) payload.get("name"));
        user.setEmail((String) payload.get("email"));
        List<String> roles = (List) ((Map<String, Object>) payload.get("realm_access")).get("roles");
        String sql = "select * from Role where status = 1 and name = ':r'";
        String internalName;
        Role internalRole = null;
        int N = roles.size(), i = 0;
        while (i != N) {
            internalName = matches.get(roles.get(i));
            internalRole = internalName == null ? null : domainManager.loadUnique(Role.class, sql.replaceFirst(":r", internalName));
            if (internalRole != null) {
                i = N;
            } else {
                i++;
            }
        }
        user.setRole(internalRole);
        user.setEnabled(Boolean.TRUE);
        return user;
    }

    @Override
    protected void afterCreationUser(User user) throws Exception {
        Long id = domainManager.newId();
        StringBuilder sql = new StringBuilder();
        sql.append("insert into ").append(DomainManager.quote(Folder.class))
                .append(" (id, status, time, name, user_id)")
                .append(" values (").append(id).append(", 1, now(), '")
                .append(user.getUsername()).append("', ").append(user.getId()).append(")");
        databaseManager.update(sql.toString());
    }
}
