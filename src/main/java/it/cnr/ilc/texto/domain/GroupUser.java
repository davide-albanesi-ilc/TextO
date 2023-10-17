package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class GroupUser extends Entity {

    private Group group;
    private User user;

    @Required
    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Required
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
