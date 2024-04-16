package it.cnr.ilc.texto.util;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.FeatureType;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
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
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import java.net.URISyntaxException;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import it.cnr.ilc.texto.domain.annotation.Indexed;

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
        builder.append(entitiesClasses.stream().map(c -> getCreationIndex(c)).filter(s -> !s.isEmpty()).collect(Collectors.joining(""))).append("\n");
        builder.append(getExtraCreation()).append("\n");
        builder.append("SET FOREIGN_KEY_CHECKS=1;\n\n");
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

    private String getCreationIndex(Class<? extends Entity> clazz) {
        StringBuilder builder = new StringBuilder();
        Entity.getters(clazz).stream()
                .filter(m -> m.isAnnotationPresent(Indexed.class))
                .map(m -> getCreationIndex(m))
                .forEach(s -> builder.append(s));
        return builder.toString();
    }

    private String getCreationIndex(Method method) {
        StringBuilder builder = new StringBuilder();
        String table = method.getDeclaringClass().getSimpleName();
        String field = getSQLField(method);
        builder.append("create index index_").append(table).append("_").append(field)
                .append(" on ").append(quote(table)).append(" (").append(field).append(");\n");
        return builder.toString();
    }

    private String getExtraCreation() {
        StringBuilder builder = new StringBuilder();
        builder.append("drop table if exists _sequence;\n")
                .append("create table _sequence (id bigint not null);\n")
                .append("insert into _sequence values(1);\n\n")
                .append("drop table if exists _access;\n")
                .append("create table _access (topic varchar(25), role varchar(25), action varchar(25), level varchar(25));\n\n")
                .append("drop table if exists _credential;\n")
                .append("create table _credential (user_id bigint unique, password varchar(50));\n")
                .append("alter table _credential add constraint fk_credential_user_id foreign key (user_id) references User(id);\n\n")
                .append("drop table if exists _text;\n")
                .append("create table _text (resource_id bigint unique, text longtext);\n")
                .append("alter table _text add constraint fk_text_resource_id foreign key (resource_id) references Resource(id);\n\n")
                .append("drop table if exists _token;\n")
                .append("create table _token (resource_id bigint, row_id bigint, number int, start int, end int, primary key (resource_id, number));\n")
                .append("alter table _token add constraint fk_token_resource_id foreign key (resource_id) references Resource(id);\n")
                .append("alter table _token add constraint fk_token_rrow_id foreign key (row_id) references `Row`(id);\n")
                .append("create index index_token_number on _token(number);\n")
                .append("create index index_token_start on _token(start);\n")
                .append("create index index_token_end on _token(end);\n")
                .append("drop table if exists _analysis;\n")
                .append("create table _analysis (resource_id bigint, number int, token varchar(255), form varchar(255), lemma varchar(255), pos varchar(255));\n")
                .append("alter table _analysis add constraint fk_analysis_token_id foreign key (resource_id, number) references _token(resource_id, number);\n")
                .append("create index index_analysis_token on _analysis(token);\n")
                .append("create index index_analysis_form on _analysis(form);\n")
                .append("create index index_analysis_lemma on _analysis(lemma);\n")
                .append("create index index_analysis_pos on _analysis(pos);");
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
        builder.append("insert into Role (id, status, time, name) values (1, 1, now(), 'Administrator');\n")
                .append("insert into Role (id, status, time, name) values (2, 1, now(), 'Editor');\n")
                .append("insert into Role (id, status, time, name) values (3, 1, now(), 'Viewer');\n")
                .append("insert into User (id, status, time, name, username, role_id, enabled) values (4, 1, now(), 'Administrator', 'admin', 1, true);\n")
                .append("insert into _credential (user_id, password) values (4, upper(sha1('Maia$23-')));\n")
                .append("insert into Folder (id, status, time, name, user_id) values (5, 1, now(), 'Administrator', 4);\n")
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
                        .append(layerId).append(", 1, now(), '").append(split[1]).append("', '").append(split[2]).append("', '").append(split[3]).append("', false);\n");
            } else if (split[0].equals(Tagset.class.getSimpleName())) {
                tagsetId = i++;
                builder.append("insert into Tagset (id, status, time, name, description) values (")
                        .append(tagsetId).append(", 1, now(), '").append(split[1]).append("', '").append(split[2]).append("');\n");
                tagsets.put(split[1], tagsetId);
            } else if (split[0].equals(TagsetItem.class.getSimpleName())) {
                builder.append("insert into TagsetItem (id, status, time, tagset_id, name, description) values (")
                        .append(i++).append(", 1, now(), ").append(tagsetId).append(", '").append(split[1]).append("', '").append(split[2]).append("');\n");
            } else if (split[0].equals(Feature.class.getSimpleName())) {
                builder.append("insert into Feature (id, status, time, layer_id, name, description, type, tagset_id) values (")
                        .append(i++).append(", 1, now(), ").append(layerId).append(", '").append(split[1]).append("', '").append(split[2]).append("', '").append(split[3]).append("', ")
                        .append(split[3].equals(FeatureType.TAGSET.toString()) ? tagsets.get(split[4]) : "null").append(");\n");
            }
        }
        builder.append("update _sequence set id = ").append(i).append(";\n");
        return builder.toString();
    }

    public static void main(String[] args) throws Exception {
        DatabaseCreator creator = new DatabaseCreator();
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(Entity.class.getPackageName()));
        reflections.getSubTypesOf(Entity.class).stream().forEach(c -> creator.addEntityClass(c));
        StringBuilder script = new StringBuilder();
        script.append(creator.getScript());
        script.append(creator.initAccess());
        script.append(creator.initEntities());
        script.append(creator.initAnalysisLayers());
        System.out.println(script);
    }

}
