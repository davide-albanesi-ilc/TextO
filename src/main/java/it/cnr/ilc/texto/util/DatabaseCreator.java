package it.cnr.ilc.texto.util;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.annotation.Required;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import java.time.LocalDate;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

/**
 *
 * @author oakgen
 */
public class DatabaseCreator {

    private static final Map<Class, String> SQL_TYPES = new HashMap<>();

    static {
        SQL_TYPES.put(String.class, "varchar(255)");
        SQL_TYPES.put(Boolean.class, "bool");
        SQL_TYPES.put(Byte.class, "tinyint");
        SQL_TYPES.put(Short.class, "smallint");
        SQL_TYPES.put(Integer.class, "int");
        SQL_TYPES.put(Long.class, "bigint");
        SQL_TYPES.put(Float.class, "float");
        SQL_TYPES.put(Double.class, "double");
        SQL_TYPES.put(LocalDate.class, "date");
        SQL_TYPES.put(LocalDateTime.class, "datetime");
        SQL_TYPES.put(Enum.class, "varchar(20)");
        SQL_TYPES.put(Entity.class, "bigint");
    }

    private final List<Class<? extends Entity>> entitiesClasses = new ArrayList<>();

    public void addEntityClass(Class<? extends Entity> clazz) {
        entitiesClasses.add(clazz);
    }

    public String getScript() {
        StringBuilder builder = new StringBuilder();
        builder.append("SET FOREIGN_KEY_CHECKS=0;\n\n");
        builder.append(entitiesClasses.stream().map(c -> getDropTable(c)).collect(Collectors.joining(""))).append("\n");
        builder.append(entitiesClasses.stream().map(c -> getCreationTable(c)).collect(Collectors.joining(""))).append("\n");
        builder.append(entitiesClasses.stream().map(c -> getCreationConstraint(c)).filter(s -> !s.isEmpty()).collect(Collectors.joining(""))).append("\n");
        builder.append(getExtraCreation()).append("\n");
        builder.append("SET FOREIGN_KEY_CHECKS=1;\n");
        return builder.toString();
    }

    private String getDropTable(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        builder.append("drop table if exists `").append(clazz.getSimpleName()).append("`;\n");
        return builder.toString();
    }

    private String getCreationTable(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        builder.append("create table `").append(clazz.getSimpleName()).append("` (\n")
                .append(" id bigint not null,\n")
                .append(" status tinyint not null,\n")
                .append(" time datetime not null,\n");
        Entity.getters(clazz).stream()
                .map(m -> getCreationField(m))
                .forEach(s -> builder.append(s));
        builder.append(" primary key(id, status, time)\n);\n");
        return builder.toString();
    }

    private String getCreationField(Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(" ").append(quote(getSQLField(method))).append(" ").append(getSQLType(method));
        if (method.isAnnotationPresent(Required.class) && method.getAnnotation(Required.class).database()) {
            builder.append(" not null");
        }
        builder.append(",\n");
        return builder.toString();
    }

    private static String getSQLField(Method method) {
        String field = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
        if (Entity.class.isAssignableFrom(method.getReturnType())) {
            field = field + "_id";
        }
        return field;
    }

    private String getSQLType(Method method) {
        Class clazz = method.getReturnType();
        if (Entity.class.isAssignableFrom(method.getReturnType())) {
            return SQL_TYPES.get(Entity.class);
        } else if (Enum.class.isAssignableFrom(method.getReturnType())) {
            return SQL_TYPES.get(Enum.class);
        } else {
            return SQL_TYPES.get(clazz);
        }
    }

    private String getCreationConstraint(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        Entity.getters(clazz).stream()
                .filter(m -> Entity.class.isAssignableFrom(m.getReturnType()))
                .map(m -> getCreationConstraint(m))
                .forEach(s -> builder.append(s));
        return builder.toString();
    }

    private String getCreationConstraint(Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append("alter table `").append(method.getDeclaringClass().getSimpleName()).append("`")
                .append(" add constraint fk_").append(method.getDeclaringClass().getSimpleName()).append("_").append(getSQLField(method))
                .append(" foreign key (").append(quote(getSQLField(method))).append(")")
                .append(" references `").append(method.getReturnType().getSimpleName()).append("`(id);\n");
        return builder.toString();
    }

    private String getExtraCreation() {
        StringBuilder builder = new StringBuilder();
        builder.append("drop table if exists _sequence;\n")
                .append("create table _sequence (id bigint not null);\n")
                .append("insert into _sequence values(0);\n\n")
                .append("drop table if exists _access;\n")
                .append("create table _access (topic varchar(25), role varchar(25), action varchar(25), level varchar(25));\n\n")
                .append("drop table if exists _credential;\n")
                .append("create table _credential (user_id bigint unique, password varchar(50));\n")
                .append("alter table _credential add constraint fk_credential_user_id foreign key (user_id) references User(id);\n\n")
                .append("drop table if exists _text;\n")
                .append("create table _text (resource_id bigint unique, text longtext);\n")
                .append("alter table _text add constraint fk_text_resource_id foreign key (resource_id) references Resource(id);\n\n")
                .append("drop table if exists _rows;\n")
                .append("create table _rows (resource_id bigint, id int, start int, end int, text longtext, primary key(resource_id, id));\n")
                .append("alter table _rows add constraint fk_rows_resource_id foreign key (resource_id) references Resource(id);\n");

        return builder.toString();
    }

    private String initAccess() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("docs/accesses.txt"));
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                builder.append("insert into _access values ('").append(line.replaceAll("\t", "', '")).append("');\n");
            }
        }
        return builder.toString();
    }

    private String initEntities() {
        StringBuilder builder = new StringBuilder();
        builder.append("insert into Role (id, status, time, name) values (1, 1, now(), 'Administrator');\n")
                .append("insert into Role (id, status, time, name) values (2, 1, now(), 'Editor');\n")
                .append("insert into Role (id, status, time, name) values (3, 1, now(), 'Viewer');\n")
                .append("insert into User (id, status, time, name, username, role_id, enabled) values (4, 1, now(), 'Administrator', 'admin', 1, true);\n")
                .append("insert into _credential (user_id, password) values (4, upper(sha1('Maia$23-')));\n")
                .append("insert into Folder (id, status, time, name, user_id) values (5, 1, now(), 'Administrator', 4);\n")
                .append("update _sequence set id = 5;");
        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        DatabaseCreator creator = new DatabaseCreator();
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(Entity.class.getPackageName()));
        reflections.getSubTypesOf(Entity.class).stream().forEach(c -> creator.addEntityClass(c));
        System.out.println(creator.getScript());
        System.out.println(creator.initAccess());
        System.out.println(creator.initEntities());
    }

}
