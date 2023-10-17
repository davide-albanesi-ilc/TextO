package it.cnr.ilc.texto.manager.access;

import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import org.springframework.core.env.Environment;

/**
 *
 * @author oakgen
 */
public class MaiaOpenAccessImplementation extends OpenAccessImplementation {

    public MaiaOpenAccessImplementation(Environment environment, DatabaseManager databaseManager, DomainManager domainManager) throws SQLException, ReflectiveOperationException, ManagerException {
        super(environment, databaseManager, domainManager);
    }

    @Override
    protected User retrieveUser() throws SQLException, ReflectiveOperationException, ManagerException {
        return domainManager.load(User.class, 4l);
    }

}
