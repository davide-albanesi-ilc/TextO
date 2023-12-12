package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Matched;
import it.cnr.ilc.texto.domain.annotation.Unique;
import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class Folder extends Entity implements Userable, Listable {

    private Folder parent;
    private String name;
    private String description;
    private User user;

    @Override
    @Required(database = false)
    public Folder getParent() {
        return parent;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    @Override
    @Unique(group = {"parent"})
    @Required
    @Matched(".+")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Required
    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

}
