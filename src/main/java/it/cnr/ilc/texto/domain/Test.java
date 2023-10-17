package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Final;
import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class Test extends Entity {

    private String rock;
    private String paper;
    private String scissor;
    private Test next;

    @Required
    public String getRock() {
        return rock;
    }

    public void setRock(String rock) {
        this.rock = rock;
    }

    @Final
    public String getPaper() {
        return paper;
    }

    public void setPaper(String paper) {
        this.paper = paper;
    }

    public String getScissor() {
        return scissor;
    }

    public void setScissor(String scissor) {
        this.scissor = scissor;
    }

    public Test getNext() {
        return next;
    }

    public void setNext(Test next) {
        this.next = next;
    }

}
