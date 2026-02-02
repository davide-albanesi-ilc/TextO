package it.cnr.ilc.texto.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author oakgen
 */
public class Tester {

    public static void main(String[] args) {
        try {
            Files.lines(Path.of("/Users/oakgen/Desktop/original_simple.txt")).forEach(System.out::println);           
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
