package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Indexed;
import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class Token extends Entity {

    private Resource resource;
    private Row row;
    private Integer number;
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

    @Required
    public Row getRow() {
        return row;
    }

    public void setRow(Row row) {
        this.row = row;
    }

    @Required
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
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
