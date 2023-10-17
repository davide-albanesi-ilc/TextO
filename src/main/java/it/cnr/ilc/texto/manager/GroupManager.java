package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Group;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class GroupManager extends EntityManager<Group> {

    @Override
    protected Class<Group> entityClass() {
        return Group.class;
    }

    @Override
    public String getLog(Group group) throws SQLException, ReflectiveOperationException, ManagerException {
        return group.getName() != null ? group.getName() : "" + group.getId();
    }

}
