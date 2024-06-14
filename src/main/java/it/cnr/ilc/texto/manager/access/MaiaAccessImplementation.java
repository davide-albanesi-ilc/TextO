package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.Folder;
import it.cnr.ilc.texto.domain.Role;
import it.cnr.ilc.texto.domain.Status;
import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.DomainManager.sqlValue;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author oakgen
 */
public class MaiaAccessImplementation extends JWTExternAccessImplementation {

    private final Map<String, String> matches = new HashMap<>();

    @Override
    protected void init() throws Exception {
        super.init();
        matches.put("ADMINISTRATOR", "Administrator");
        matches.put("SUPERVISOR", "Editor");
        matches.put("LEXICOGRAPHER", "Viewer");
        matches.put("ANNOTATOR", "Editor");
        matches.put("LEXICOGRAPHER_ANNOTATOR", "Editor");
        matches.put("VIEWER", "Viewer");
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
        StringBuilder builder = new StringBuilder();
        builder.append("select * from ").append(quote(Role.class))
                .append(" where status = ").append(Status.VALID.ordinal())
                .append(" and name = ").append(sqlValue(roleName));
        Role role = domainManager.loadUnique(Role.class, builder.toString());
        user.setRole(role);
        user.setEnabled(Boolean.TRUE);
        return user;
    }

    @Override
    protected void afterCreationUser(User user) throws Exception {
        if (!Boolean.TRUE.equals(environment.getProperty("folder.shared-home", Boolean.class))) {
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
