package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Indexed;
import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class Row extends Entity {

    private Resource resource;
    private Section section;
    private Integer number;
    private Integer relative;
    private Integer start;
    private Integer end;

    @Required
    @Indexed(name = "number", group = {"number"})
    @Indexed(name = "offset_start", group = {"start"})
    @Indexed(name = "offset_end", group = {"end"})
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    @Required
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getRelative() {
        return relative;
    }

    public void setRelative(Integer relative) {
        this.relative = relative;
    }

    @Required
    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    @Required
    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

}
