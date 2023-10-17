package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import it.cnr.ilc.texto.manager.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("authentication")
public class AuthenticationController extends Controller {

    @Autowired
    private UserManager userManager;

    @PostMapping("login")
    public String login(@RequestBody Credential credential) throws Exception {
        Logger logger = LoggerFactory.getLogger(Controller.class);
        User user = userManager.authenticate(credential.username, credential.password);
        if (user == null) {
            logger.info("(" + credential.username + ") login authentication failed");
            throw new AuthorizationException("authentication failed");
        }
        String token = accessManager.startSession(user);
        logger.info("(" + credential.username + ") [" + token + "] login");
        return token;
    }

    @GetMapping("keepAlive")
    public void keepAlive() throws Exception {
        logManager.setMessage("keep alive");
    }

    @GetMapping("logout")
    public void logout() throws Exception {
        logManager.setMessage("logout");
        accessManager.endSession();
    }

    public static record Credential(String username, String password) {

    }
}
