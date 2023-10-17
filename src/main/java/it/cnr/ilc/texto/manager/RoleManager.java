package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Role;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class RoleManager extends EntityManager<Role> {

    @Override
    protected Class<Role> entityClass() {
        return Role.class;
    }

        @Override
    public String getLog(Role role) throws SQLException, ReflectiveOperationException, ManagerException {
        return role.getName() != null ? role.getName() : "" + role.getId();
    }
}
