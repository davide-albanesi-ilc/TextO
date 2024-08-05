package it.cnr.ilc.texto.util;

import it.cnr.ilc.texto.domain.Entity;
import static it.cnr.ilc.texto.manager.DomainManager.quoteHistory;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

/**
 *
 * @author oakgen
 */
public class Tester {

    public static void main(String[] args) throws Exception {
        StringBuilder builder = new StringBuilder();
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(Entity.class.getPackageName()));
        reflections.getSubTypesOf(Entity.class).stream().forEach(c -> {
            builder.append("alter table ").append(quoteHistory(c)).append(" drop primary key;\n");
            builder.append("alter table ").append(quoteHistory(c)).append(" add index(id);\n");
            builder.append("\n");
        });
        System.out.println(builder);
    }
}
