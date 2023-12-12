package it.cnr.ilc.texto.util;

/**
 *
 * @author oakgen
 */
public class Tester {

    public static void main(String[] args) throws Exception {
        String pattern = "[a-zA-Z0-9@._-]{4,}";
        System.out.println("davide@gmail.com".matches(pattern));
    }
}
