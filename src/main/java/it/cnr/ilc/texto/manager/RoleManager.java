package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Role;
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
    public String getLog(Role role) {
        return role.getName() != null ? role.getName() : "" + role.getId();
    }
}
