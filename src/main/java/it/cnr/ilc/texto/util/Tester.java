package it.cnr.ilc.texto.util;

import java.util.Random;

/**
 *
 * @author oakgen
 */
public class Tester {

    public static void main(String[] args) {
        Random r = new Random(System.currentTimeMillis());
        int[] a = new int[100];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
//        for (int i = 0; i < a.length; i++) {
//            System.out.print(a[i] + " ");
//        }
//        System.out.println();
        for (int i = 0; i < a.length; i++) {
            int j = r.nextInt(a.length);
            int s = a[i];
            a[i] = a[j];
            a[j] = s;
        }
//        for (int i = 0; i < a.length; i++) {
//            System.out.print(a[i] + " ");
//        }
//        System.out.println();
        sort(a);
//        for (int i = 0; i < a.length; i++) {
//            System.out.print(a[i] + " ");
//        }
//        System.out.println();

    }

    private static void sort(int[] a) {
        int i = 0;
        int c;
        while (i < a.length) {
            c = 0;
            while (a[i] != i) {
                int j = a[i];
                int s = a[i];
                a[i] = a[j];
                a[j] = s;
                c++;
            }
            if (c != 0) {
                System.out.println("ciclo " + c);
            }
            i++;
        }
    }
}
