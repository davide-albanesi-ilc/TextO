package it.cnr.ilc.texto.domain;

import java.util.Map;

/**
 *
 * @author oakgen
 */
public class Offset {

    private Integer start;
    private Integer end;

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

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "[" + start + ":" + end + "]";
    }
}
