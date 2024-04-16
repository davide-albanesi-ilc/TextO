package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import java.util.Base64;
import java.util.Random;

/**
 *
 * @author oakgen
 */
public class SessionIdInnternalAccessImplementation extends InternalAccessImplementation {

    private static final int KEY_BUFFER_SIZE = 24;
    private final Random random = new Random(System.currentTimeMillis());

    @Override
    protected String retrieveToken(String token) throws Exception {
        if (!token.toLowerCase().startsWith("sessionid")) {
            throw new AuthorizationException("invalid authorization parameter");
        }
        return token.substring(10);
    }

    @Override
    protected String generateToken() {
        byte[] buffer = new byte[KEY_BUFFER_SIZE];
        random.nextBytes(buffer);
        String key = Base64.getUrlEncoder().encodeToString(buffer);
        return key;
    }

}
