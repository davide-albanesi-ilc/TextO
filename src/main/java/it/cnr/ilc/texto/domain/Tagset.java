package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Unique;
import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class Tagset extends Entity {

    private String name;
    private String description;
    private TagsetItem defaultItem;

    @Unique
    @Required
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

    public TagsetItem getDefaultItem() {
        return defaultItem;
    }

    public void setDefaultItem(TagsetItem defaultItem) {
        this.defaultItem = defaultItem;
    }

}
