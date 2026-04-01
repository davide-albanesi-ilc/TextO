package it.cnr.ilc.texto.manager.access;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author oakgen
 */
public class JWTInternalAccessImplementation extends InternalAccessImplementation {

    private String publicKey;
    private String privateKey;

    @Override
    protected void init() throws AuthorizationException {
        try (InputStream input = JWTExternalAccessImplementation.class.getResourceAsStream("/public.pem")) {
            publicKey = new String(input.readAllBytes())
                    .replaceAll("-----.*-----", "")
                    .replaceAll("\n", "");
        } catch (IOException e) {
            throw new AuthorizationException("public key not found");
        }
        try (InputStream input = JWTExternalAccessImplementation.class.getResourceAsStream("/private8.pem")) {
            privateKey = new String(input.readAllBytes())
                    .replaceAll("-----.*-----", "")
                    .replaceAll("\n", "");
        } catch (IOException e) {
            throw new AuthorizationException("private key not found");
        }
    }

    @Override
    protected String retrieveToken
    (String token) throws Exception {
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
            throw new AuthorizationException("jwt validation error");
        }
    }

    @Override
    protected String generateToken(User user) throws Exception {
        long timeout = Long.parseLong(environment.getProperty("access.session-timeout", "1800")) * 1000;
        EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey key = kf.generatePrivate(keySpec);
        String jwtToken = Jwts.builder()
                .claim("username", user.getUsername())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(timeout, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
        return jwtToken;
    }

}
