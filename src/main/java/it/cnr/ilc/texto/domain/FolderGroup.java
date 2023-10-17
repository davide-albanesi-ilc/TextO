package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class FolderGroup extends Entity {

    private Folder folder;
    private Group group;
    private Action action;

    @Required
    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    @Required
    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Required
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
