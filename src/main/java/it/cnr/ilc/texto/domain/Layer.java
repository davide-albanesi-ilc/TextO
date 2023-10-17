package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;
import it.cnr.ilc.texto.domain.annotation.Unique;

/**
 *
 * @author oakgen
 */
public class Layer extends Entity {

    private String name;
    private String description;
    private String color;
    private Boolean overlapping = Boolean.FALSE;

    @Required
    @Unique
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
    @Unique
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Required
    public Boolean getOverlapping() {
        return overlapping;
    }

    public void setOverlapping(Boolean overlapping) {
        this.overlapping = overlapping;
    }

}
