package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Group;
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
    public String getLog(Group group) {
        return group.getName() != null ? group.getName() : "" + group.getId();
    }

}
