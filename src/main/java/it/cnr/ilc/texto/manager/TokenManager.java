package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Token;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class TokenManager extends EntityManager<Token> {

    @Lazy
    @Autowired
    private ResourceManager resourceManager;

    @Override
    protected Class<Token> entityClass() {
        return Token.class;
    }

    @Override
    public String getLog(Token token) throws SQLException, ReflectiveOperationException {
        if (token.getResource() == null || token.getStart() == null || token.getEnd() == null || token.getNumber() == null) {
            return "" + token.getId();
        } else {
            return resourceManager.getLog(resourceManager.load(token.getResource().getId())) + " " + token.getNumber();
        }
    }

    @Check
    public void empty(Token previous, Token token) throws ManagerException {
        if (token.getStart().equals(token.getEnd())) {
            throw new ManagerException("empty token");
        }
    }

}
