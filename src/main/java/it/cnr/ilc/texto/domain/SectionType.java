package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class SectionType extends Entity {

    private Resource resource;
    private SectionType parent;
    private String name;

    @Required
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public SectionType getParent() {
        return parent;
    }

    public void setParent(SectionType parent) {
        this.parent = parent;
    }

    @Required
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
