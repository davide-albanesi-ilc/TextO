package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class FolderUser extends Entity {

    private Folder folder;
    private User user;
    private Action action;

    @Required
    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    @Required
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Required
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
