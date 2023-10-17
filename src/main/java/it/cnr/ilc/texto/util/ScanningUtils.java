package it.cnr.ilc.texto.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 * @author oakgen
 */
public class ScanningUtils {

    public static List<Class> getClasses(String packageName) throws IOException, ClassNotFoundException {
        try (JarFile jar = getJarFile()) {
            if (jar != null) {
                return doGetClasses(packageName, jar);
            } else {
                return doGetClasses(packageName);
            }
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    private static JarFile getJarFile() throws IOException {
        String dirtyPath = ScanningUtils.class.getResource("").toString();
        if (dirtyPath.startsWith("jar")) {
            String jarPath = dirtyPath.replaceAll("^.*file:", "");
            jarPath = jarPath.replaceAll("jar!.*", "jar");
            jarPath = jarPath.replaceAll("%20", " ");
            return new JarFile(jarPath);
        } else {
            return null;
        }
    }

    private static List<Class> doGetClasses(String packageName) throws IOException, ClassNotFoundException, URISyntaxException {
        List<Class> list = new ArrayList<>();
        URL url = ScanningUtils.class.getResource("/" + packageName.replaceAll("\\.", "/"));
        for (String line : new File(url.toURI()).list()) {
            if (line.endsWith(".class")) {
                list.add(Class.forName(packageName + "." + line.substring(0, line.lastIndexOf("."))));
            }
        }
        return list;
    }

    private static List<Class> doGetClasses(String packageName, JarFile jar) throws IOException, ClassNotFoundException {
        List<Class> list = new ArrayList<>();
        String packagePath = packageName.replaceAll("\\.", "/"), name;
        int index;
        for (Iterator<JarEntry> iterator = jar.entries().asIterator(); iterator.hasNext();) {
            JarEntry entry = iterator.next();
            if (entry.getRealName().endsWith(".class")) {
                index = entry.getRealName().lastIndexOf("/");
                if (entry.getRealName().substring(0, index).endsWith(packagePath)) {
                    name = packageName + "." + entry.getRealName().substring(index + 1, entry.getRealName().length() - 6);
                    list.add(Class.forName(name));
                }
            }
        }
        return list;
    }

}
