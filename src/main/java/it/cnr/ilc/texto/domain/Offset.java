package it.cnr.ilc.texto.domain;

import java.util.Map;

/**
 *
 * @author oakgen
 */
public class Offset {

    public static Offset fromMap(Map<String, String> params) {
        Offset offset = new Offset();
        if (params.containsKey("start")) {
            offset.start = Integer.valueOf(params.get("start"));
        }
        if (params.containsKey("end")) {
            offset.end = Integer.valueOf(params.get("end"));
        }
        return offset;
    }

    public static Offset fromValues(Integer start, Integer end) {
        Offset offset = new Offset();
        offset.start = start;
        offset.end = end;
        return offset;
    }

    public Integer start;
    public Integer end;

    @Override
    public String toString() {
        return "[" + start + ":" + end + "]";
    }
}
