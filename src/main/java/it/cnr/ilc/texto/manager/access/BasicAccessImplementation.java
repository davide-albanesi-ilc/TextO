package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import java.util.Base64;

/**
 *
 * @author oakgen
 */
public class BasicAccessImplementation extends ExternalAccessImplementation {

    @Override
    protected String retrieveToken(String token) throws Exception {
        if (!token.toLowerCase().startsWith("basic ")) {
            throw new AuthorizationException("invalid authorization parameter");
        }
        token = new String(Base64.getDecoder().decode(token.substring(6)));
        String[] credentials = token.split(":");
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(User.class)).append(" u ")
                .append("join _credential c on u.id = c.user_id ")
                .append("where username = '").append(credentials[0]).append("'")
                .append(" and password = upper(sha1('").append(credentials[1]).append("))'");
        User user = domainManager.loadUnique(User.class, sql.toString());
        if (user == null) {
            throw new AuthorizationException("authentication failed");
        }
        return token;
    }

    @Override
    protected User retrieveUser(String token) throws Exception {
        token = new String(Base64.getDecoder().decode(token.substring(6)));
        String[] credentials = token.split(":");
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(User.class)).append(" u ")
                .append("join _credential c on u.id = c.user_id ")
                .append("where username = '").append(credentials[0]).append("'")
                .append(" and password = upper(sha1('").append(credentials[1]).append("))'");
        return domainManager.loadUnique(User.class, sql.toString());
    }

}
