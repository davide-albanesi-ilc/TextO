package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Token;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.TokenManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("token")
public class TokenController extends EntityController<Token> {

    @Autowired
    private TokenManager tokenManager;

    @Override
    protected EntityManager<Token> entityManager() {
        return tokenManager;
    }

    @Override
    protected Class<Token> entityClass() {
        return Token.class;
    }

    @Override
    protected void checkAccess(Token token, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (token.getResource() != null) {
            accessManager.checkAccess(token.getResource(), action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }

}
