package it.cnr.ilc.texto.util;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.FeatureType;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.domain.Test;
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
import java.time.LocalDate;
import java.net.URISyntaxException;
import it.cnr.ilc.texto.domain.annotation.Indexed;
import it.cnr.ilc.texto.domain.annotation.Unique;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Stream;

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
        SQL_TYPES.put(LocalDateTime.class, "datetime(6)");
        SQL_TYPES.put(Enum.class, "varchar(20)");
        SQL_TYPES.put(Entity.class, "bigint");
    }

    private final List<Class<? extends Entity>> entitiesClasses = new ArrayList<>();

    public void addEntityClass(Class<? extends Entity> clazz) {
        entitiesClasses.add(clazz);
    }

    public void removeEntityClass(Class<? extends Entity> clazz) {
        entitiesClasses.remove(clazz);
    }

    public String getScript() {
        StringBuilder builder = new StringBuilder();
        builder.append("SET FOREIGN_KEY_CHECKS=0;\n\n");
        builder.append(entitiesClasses.stream().map(c -> getCreationTable(c)).collect(Collectors.joining("\n"))).append("\n");
        builder.append(entitiesClasses.stream().map(c -> getCreationHistory(c)).collect(Collectors.joining("\n"))).append("\n");
        builder.append(entitiesClasses.stream().map(c -> getCreationConstraint(c)).filter(s -> !s.isEmpty()).collect(Collectors.joining(""))).append("\n");
        builder.append(entitiesClasses.stream().map(c -> getCreationUnique(c)).filter(s -> !s.isEmpty()).collect(Collectors.joining(""))).append("\n");
        builder.append(entitiesClasses.stream().map(c -> getCreationIndex(c)).filter(s -> !s.isEmpty()).collect(Collectors.joining(""))).append("\n");
        builder.append(getExtraCreation()).append("\n");
        builder.append("SET FOREIGN_KEY_CHECKS=1;\n\n");
        return builder.toString();
    }

    private String getCreationTable(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        builder.append("drop table if exists `").append(clazz.getSimpleName()).append("`;\n");
        builder.append("create table `").append(clazz.getSimpleName()).append("` (\n")
                .append(" id bigint not null,\n")
                .append(" status tinyint not null,\n")
                .append(" time datetime(6) not null,\n");
        Entity.getters(clazz).stream()
                .map(m -> getCreationField(m))
                .forEach(s -> builder.append(s));
        builder.append(" primary key(id)\n);\n");
        return builder.toString();
    }

    private String getCreationField(Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(" `").append(getSQLField(method)).append("` ").append(getSQLType(method));
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

    private String getCreationHistory(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        builder.append("drop table if exists `").append(clazz.getSimpleName()).append("_`;\n");
        builder.append("create table `").append(clazz.getSimpleName()).append("_` (\n")
                .append(" id bigint not null,\n")
                .append(" status tinyint not null,\n")
                .append(" time datetime(6) not null,\n");
        Entity.getters(clazz).stream()
                .map(m -> getCreationField(m))
                .forEach(s -> builder.append(s));
        builder.append(" primary key(id, status, time)\n);\n");
        return builder.toString();
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
                .append(" foreign key (`").append(getSQLField(method)).append("`)")
                .append(" references `").append(method.getReturnType().getSimpleName()).append("`(id);\n");
        return builder.toString();
    }

    private String getCreationIndex(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        Entity.getters(clazz).stream()
                .filter(m -> m.isAnnotationPresent(Indexed.class))
                .forEach(m -> builder.append(getCreationIndex(m)));
        return builder.toString();
    }

    private String getCreationIndex(Method method) {
        Indexed[] indexeds = method.getAnnotationsByType(Indexed.class);
        StringBuilder builder = new StringBuilder();
        for (Indexed indexed : indexeds) {
            Class<? extends Entity> clazz = (Class<? extends Entity>) method.getDeclaringClass();
            String table = clazz.getSimpleName();
            String field = getSQLField(method);
            String name = indexed.name();
            Stream<String> groupStream = Arrays.stream(indexed.group()).map(g -> getSQLField(getGetter(clazz, g)));
            String group = Stream.concat(Stream.of(field), groupStream).collect(Collectors.joining(", "));
            builder.append("create index index_").append(table).append("_").append(name)
                    .append(" on `").append(table).append("` (").append(group).append(");\n");
        }
        return builder.toString();
    }

    private String getCreationUnique(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        Entity.getters(clazz).stream()
                .filter(m -> m.isAnnotationPresent(Unique.class))
                .forEach(m -> builder.append(getCreationUnique(m)));
        return builder.toString();
    }

    private String getCreationUnique(Method method) {
        Unique[] uniques = method.getAnnotationsByType(Unique.class);
        StringBuilder builder = new StringBuilder();
        for (Unique unique : uniques) {
            String table = method.getDeclaringClass().getSimpleName();
            String field = getSQLField(method);
            String name = unique.name().isEmpty() ? field : unique.name();
            Class<? extends Entity> clazz = (Class<? extends Entity>) method.getDeclaringClass();
            Stream<String> groupStream = Arrays.stream(unique.group()).map(g -> getSQLField(getGetter(clazz, g)));
            String group = Stream.concat(Stream.of(field), groupStream).collect(Collectors.joining(", "));
            builder.append("alter table `").append(table).append("` add constraint uq_").append(table).append("_").append(name)
                    .append(" unique (").append(group).append(");\n");
        }
        return builder.toString();
    }

    private Method getGetter(Class<? extends Entity> clazz, String field) {
        try {
            return clazz.getDeclaredMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getExtraCreation() {
        StringBuilder builder = new StringBuilder();
        builder.append("drop table if exists _sequence;\n")
                .append("create table _sequence (id bigint not null);\n")
                .append("insert into _sequence values(1);\n\n")
                .append("drop table if exists _access;\n")
                .append("create table _access (topic varchar(25), role varchar(25), action varchar(25), level varchar(25));\n\n")
                .append("drop table if exists _credential;\n")
                .append("create table _credential (user_id bigint primary key, password varchar(50));\n")
                .append("drop table if exists _text;\n")
                .append("create table _text (resource_id bigint primary key, text longtext);\n")
                .append("drop table if exists _tokenannotation;\n")
                .append("create table _tokenannotation (token_id bigint, annotation_id bigint);\n")
                .append("alter table _tokenannotation add primary key pk_tokenannotation (token_id, annotation_id);\n")
                .append("alter table _tokenannotation add constraint fk_tokenannotation_token foreign key (token_id) references Token (id);\n")
                .append("alter table _tokenannotation add constraint fk_tokenannotation_annotation foreign key (annotation_id) references Annotation (id);\n");
        return builder.toString();
    }

    private String initAccess() throws IOException, URISyntaxException {
        List<String> lines = Files.readAllLines(Path.of(DatabaseCreator.class.getResource("/accesses.init").toURI()));
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
        builder.append("insert into Role (id, status, time, name) values (1, 1, now(6), 'Administrator');\n")
                .append("insert into Role (id, status, time, name) values (2, 1, now(6), 'Editor');\n")
                .append("insert into Role (id, status, time, name) values (3, 1, now(6), 'Viewer');\n")
                .append("insert into User (id, status, time, name, username, role_id, enabled) values (4, 1, now(6), 'Administrator', 'admin', 1, true);\n")
                .append("insert into _credential (user_id, password) values (4, upper(sha1('password')));\n")
                .append("insert into Folder (id, status, time, name, user_id) values (5, 1, now(6), 'Administrator', 4);\n")
                .append("update _sequence set id = 6;\n");
        return builder.toString();
    }

    public String initAnalysisLayers() throws IOException, URISyntaxException {
        List<String> lines = Files.readAllLines(Path.of(DatabaseCreator.class.getResource("/analysis.init").toURI()));
        StringBuilder builder = new StringBuilder();
        long layerId = -1;
        long tagsetId = -1;
        int i = 6;
        Map<String, Long> tagsets = new HashMap<>();
        String[] split;
        for (String line : lines) {
            split = line.split("\t");
            if (split[0].equals(Layer.class.getSimpleName())) {
                layerId = i++;
                builder.append("insert into Layer (id, status, time, name, description, color, overlapping) values (")
                        .append(layerId).append(", 1, now(6), '").append(split[1]).append("', '").append(split[2]).append("', '").append(split[3]).append("', false);\n");
            } else if (split[0].equals(Tagset.class.getSimpleName())) {
                tagsetId = i++;
                builder.append("insert into Tagset (id, status, time, name, description) values (")
                        .append(tagsetId).append(", 1, now(6), '").append(split[1]).append("', '").append(split[2]).append("');\n");
                tagsets.put(split[1], tagsetId);
            } else if (split[0].equals(TagsetItem.class.getSimpleName())) {
                builder.append("insert into TagsetItem (id, status, time, tagset_id, name, description) values (")
                        .append(i++).append(", 1, now(6), ").append(tagsetId).append(", '").append(split[1]).append("', '").append(split[2]).append("');\n");
            } else if (split[0].equals(Feature.class.getSimpleName())) {
                builder.append("insert into Feature (id, status, time, layer_id, name, description, type, tagset_id) values (")
                        .append(i++).append(", 1, now(6), ").append(layerId).append(", '").append(split[1]).append("', '").append(split[2]).append("', '").append(split[3]).append("', ")
                        .append(split[3].equals(FeatureType.TAGSET.toString()) ? tagsets.get(split[4]) : "null").append(");\n");
            }
        }
        builder.append("update _sequence set id = ").append(i).append(";\n");
        return builder.toString();
    }

    public static void save(Path path, CharSequence script) throws IOException {
        Files.writeString(path, script);
    }

    public static void execute(Connection connection, CharSequence script) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            String[] lines = script.toString().split("\n");
            int count = 0;
            for (String line : lines) {
                System.out.println(count++ + " / " + lines.length);
                if (!line.isBlank()) {
                    try {
                        statement.execute(line);
                    } catch (SQLException e) {
                        System.err.println(line);
                        throw e;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost/texto?characterEncoding=UTF-8&serverTimezone=Europe/Rome", "root", "root")) {
            DatabaseCreator creator = new DatabaseCreator();
//            Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(Entity.class.getPackageName()));
//            reflections.getSubTypesOf(Entity.class).stream().forEach(c -> creator.addEntityClass(c));
            creator.addEntityClass(Test.class);
            StringBuilder script = new StringBuilder();
            script.append(creator.getScript());
//            script.append(creator.initAccess());
//            script.append(creator.initEntities());
//            script.append(creator.initAnalysisLayers());
            save(Path.of("/Users/oakgen/Desktop/create.sql"), script);
//            execute(connection, script);
        }
    }

}
