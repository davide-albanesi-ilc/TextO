package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Unique;
import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class TagsetItem extends Entity {

    private String name;
    private String description;
    private Tagset tagset;

    @Unique(group = {"tagset"})
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

    @Required
    public Tagset getTagset() {
        return tagset;
    }

    public void setTagset(Tagset tagset) {
        this.tagset = tagset;
    }

}
