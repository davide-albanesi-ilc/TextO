package it.cnr.ilc.texto.manager.access;

import tools.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.DomainManager.sqlValue;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 *
 * @author oakgen
 */
public abstract class JWTExternalAccessImplementation extends ExternalAccessImplementation {

    private final ObjectMapper mapper = new ObjectMapper();
    private String publicKey;

    @Override
    protected void init() throws AuthorizationException {
        try {
            publicKey = new String(Files.readAllBytes(Paths.get(environment.getProperty("jwt.public-key", "public.pem"))))
                    .replaceAll("-----.*-----", "")
                    .replaceAll("\n", "");
        } catch (IOException e) {
            throw new AuthorizationException("public key not found");
        }
    }

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

    private Claims validate(String jwtToken) throws AuthorizationException {
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
                .append(" where username = ").append(sqlValue(retrieveUsername(payload)));
        User user = domainManager.loadUnique(User.class, sql.toString());
        if (user == null) {
            user = retrieveUser(payload);
            domainManager.create(user);
            databaseManager.getConnection().commit();
        }
        return user;
    }

    protected abstract String retrieveUsername(Map<String, Object> payload) throws Exception;

    protected abstract User retrieveUser(Map<String, Object> payload) throws Exception;

}
