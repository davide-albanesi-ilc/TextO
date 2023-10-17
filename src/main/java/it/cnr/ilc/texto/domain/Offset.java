package it.cnr.ilc.texto.domain;

/**
 *
 * @author oakgen
 */
public class Offset {

    public Integer start;
    public Integer end;

    @Override
    public String toString() {
        return "[" + start + ":" + end + "]";
    }
}
