package it.cnr.ilc.texto.manager.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 *
 * @author oakgen
 */
public abstract class JWTExternAccessImplementation extends ExternalAccessImplementation {

    private final ObjectMapper mapper = new ObjectMapper();
    private String publicKey;

    @Override
    protected String retrieveToken(String token) throws Exception {
        if (!token.toLowerCase().startsWith("bearer")) {
            throw new AuthorizationException("invalid authorization parameter");
        }
        token = token.substring(6).trim();
        validate(token);
        String[] chunks = token.split("\\.");
        return chunks[1];
    }

    private String loadPublicKey() throws AuthorizationException {
        if (publicKey == null) {
            try (InputStream input = JWTExternAccessImplementation.class.getResourceAsStream("/public.pem")) {
                publicKey = new String(input.readAllBytes())
                        .replaceAll("-----.*-----", "")
                        .replaceAll("\n", "");
            } catch (IOException e) {
                throw new AuthorizationException("public key not found");
            }
        }
        return publicKey;
    }

    private Claims validate(String jwtToken) throws AuthorizationException {
        loadPublicKey();
        String algorithm = environment.getProperty("jwt.algorithm", "RSA");
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            PublicKey key = keyFactory.generatePublic(keySpec);
            Jws<Claims> jwt = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(jwtToken);
            return jwt.getPayload();
        } catch (Exception e) {
            throw new AuthorizationException("jwt error");
        }
    }

    @Override
    protected synchronized User retrieveUser(String token) throws Exception {
        token = new String(Base64.getUrlDecoder().decode(token));
        Map<String, Object> payload = mapper.readValue(token, Map.class);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(User.class))
                .append(" where username = '").append(retrieveUsername(payload)).append("'");
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
