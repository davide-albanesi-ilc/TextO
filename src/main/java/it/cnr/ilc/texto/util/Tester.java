package it.cnr.ilc.texto.util;

import java.util.LinkedHashMap;

/**
 *
 * @author oakgen
 */
public class Tester {

    public static void main(String[] args) throws Exception {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put("val" + i, i);
        }
        map.put("val2", map.get("val2")+10);
        System.out.println(map.values());
    }
}
