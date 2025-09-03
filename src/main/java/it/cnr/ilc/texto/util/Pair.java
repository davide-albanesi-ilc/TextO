package it.cnr.ilc.texto.util;

/**
 *
 * @author oakgen
 */
public class Pair<F, S> {

    private final F f;
    private final S s;

    public Pair(F f, S s) {
        this.f = f;
        this.s = s;
    }

    public F getFirst() {
        return f;
    }

    public S getSecond() {
        return s;
    }

}
