package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Role;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.DomainManager.sqlValue;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.Environment;

/**
 *
 * @author oakgen
 */
public class MaiaAccessImplementation extends JWTExternAccessImplementation {

    private final Map<String, String> matches = new HashMap<>();

    public MaiaAccessImplementation(Environment environment, DatabaseManager databaseManager, DomainManager entityManager) {
        super(environment, databaseManager, entityManager);
        matches.put("AMMINISTRATORE", "Administrator");
        matches.put("SUPERVISORE", "Editor");
        matches.put("LESSICOGRAFO", "Viewer");
        matches.put("ANNOTATORE", "Editor");
        matches.put("LESSICOGRAFO_ANNOTATORE", "Editor");
        matches.put("UTENTE_VIEWER", "Viewer");
    }

    @Override
    protected String retrieveUsername(Map<String, Object> payload) throws Exception {
        return (String) payload.get("username");
    }

    @Override
    protected User retrieveUser(Map<String, Object> payload) throws Exception {
        User user = new User();
        user.setUsername((String) payload.get("username"));
        user.setName((String) payload.get("name"));
        user.setEmail((String) payload.get("email"));
        String roleName = matches.get((String) payload.get("role"));
        String sql = "select * from Role where status = 1 and name = " + sqlValue(roleName);
        Role role = domainManager.loadUnique(Role.class, sql);
        user.setRole(role);
        user.setEnabled(Boolean.TRUE);
        return user;
    }

    @Override
    protected void afterCreationUser(User user) throws Exception {
        if (!Boolean.TRUE.equals(environment.getProperty("home.shared", Boolean.class))) {
            Long id = domainManager.newId();
            StringBuilder sql = new StringBuilder();
            sql.append("insert into ").append(quote(Folder.class))
                    .append(" (id, status, time, name, user_id)")
                    .append(" values (").append(id).append(", 1, now(), '")
                    .append(user.getUsername()).append("', ").append(user.getId()).append(")");
            databaseManager.update(sql.toString());
        }
    }

}
