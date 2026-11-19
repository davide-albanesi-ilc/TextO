package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Status;
import it.cnr.ilc.texto.domain.annotation.Final;
import it.cnr.ilc.texto.domain.annotation.Matched;
import it.cnr.ilc.texto.domain.annotation.Required;
import it.cnr.ilc.texto.domain.annotation.Unique;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.annotation.Trigger.Event;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component

public class DomainManager extends Manager {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @Autowired
    protected DatabaseManager databaseManager;
    @Autowired
    protected MonitorManager monitorManager;
    @Autowired
    protected AccessManager accessManager;

    private final Map<Class, Descriptor> descriptors = new HashMap<>();
    private final Cache cache = new Cache();

    @PostConstruct
    private void initDescriptors() throws ReflectiveOperationException {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(Entity.class.getPackageName()));
        Collection<Class<? extends Entity>> classes = reflections.getSubTypesOf(Entity.class);
        for (Class<? extends Entity> clazz : classes) {
            descriptors.put(clazz, new Descriptor(clazz));
        }
    }

    public <E extends Entity> List<E> load(Class<E> clazz) throws SQLException, ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(descriptor.clazz));
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        List<E> entities = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            entities.add(toEntity(record, descriptor));
        }
        return cache.put(entities);
    }

    public <E extends Entity> E load(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException {
        E entity = cache.get(clazz, id);
        if (entity != null) {
            return entity;
        }
        Descriptor<E> descriptor = descriptors.get(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(descriptor.clazz))
                .append(" where id = ").append(id);
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        if (records.isEmpty()) {
            return null;
        }
        entity = toEntity(records.get(0), descriptor);
        return cache.put(entity);
    }

    public <E extends Entity> List<E> load(Class<E> clazz, String sql) throws SQLException, ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get(clazz);
        List<Map<String, Object>> records = databaseManager.query(sql);
        List<E> entities = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            entities.add(toEntity(record, descriptor));
        }
        return cache.put(entities);
    }

    public <E extends Entity> E loadUnique(Class<E> clazz, String sql) throws SQLException, ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get(clazz);
        List<Map<String, Object>> records = databaseManager.query(sql);
        if (records.isEmpty()) {
            return null;
        } else if (records.size() > 1) {
            throw new SQLException("not unique query");
        } else if (records.size() == 1) {
            return cache.put(toEntity(records.get(0), descriptor));
        } else {
            return null;
        }
    }

    public <E extends Entity> E create(Class<E> clazz) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get(clazz);
        E entity = descriptor.constructor.newInstance();
        entity.setId(newId());
        entity.setStatus(Status.VALID);
        entity.setTime(now());
        executeTriggers(Event.PRE_CREATE, null, entity);
        preCheck(entity);
        sqlInsert(entity);
        executeTriggers(Event.POST_CREATE, null, entity);
        return cache.put(entity);
    }

    public <E extends Entity> E create(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        entity.setId(newId());
        entity.setStatus(Status.VALID);
        entity.setTime(now());
        executeTriggers(Event.PRE_CREATE, null, entity);
        preCheck(entity);
        sqlInsert(entity);
        executeTriggers(Event.POST_CREATE, null, entity);
        return cache.put(entity);
    }

    public <E extends Entity> void create(List<E> entities) throws SQLException, ReflectiveOperationException, ManagerException {
        monitorManager.addMax(entities.size());
        long id = newId(entities.size());
        for (E entity : entities) {
            entity.setId(id++);
            entity.setStatus(Status.VALID);
            entity.setTime(now());
            executeTriggers(Event.PRE_CREATE, null, entity);
            preCheck(entity);
            sqlInsert(entity);
            executeTriggers(Event.POST_CREATE, null, entity);
            cache.put(entity);
            monitorManager.next();
        }
    }

    public <E extends Entity> E update(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        E previous = getPrevious((Class<E>) entity.getClass(), entity.getId());
        if (previous == null) {
            throw new ManagerException("entity not found");
        }
        previous.setStatus(Status.HISTORY);
        entity.setStatus(Status.VALID);
        entity.setTime(now());
        changesCheck(previous, entity);
        executeTriggers(Event.PRE_UPDATE, previous, entity);
        preCheck(previous, entity);
        sqlHistory(previous);
        sqlUpdate(entity);
        executeTriggers(Event.POST_UPDATE, previous, entity);
        return cache.put(entity);
    }

    public <E extends Entity> E update(Class<E> clazz, Long id, Map<String, Object> values) throws SQLException, ReflectiveOperationException, ManagerException {
        E previous = getPrevious(clazz, id);
        if (previous == null) {
            throw new ManagerException("entity not found");
        }
        E entity = merge(previous, values);
        previous.setStatus(Status.HISTORY);
        entity.setStatus(Status.VALID);
        entity.setTime(now());
        changesCheck(previous, entity);
        executeTriggers(Event.PRE_UPDATE, previous, entity);
        preCheck(previous, entity);
        sqlHistory(previous);
        sqlUpdate(entity);
        executeTriggers(Event.POST_UPDATE, previous, entity);
        return cache.put(entity);
    }

    public <E extends Entity> E remove(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        E previous = getPrevious(clazz, id);
        if (previous == null) {
            throw new ManagerException("entity not found");
        }
        previous.setStatus(Status.REMOVED);
        previous.setTime(now());
        executeTriggers(Event.PRE_REMOVE, previous, null);
        sqlHistory(previous);
        sqlDelete(previous);
        executeTriggers(Event.POST_REMOVE, previous, null);
        return cache.remove(previous);
    }

    public <E extends Entity> List<E> history(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get(clazz);
        List<E> entities = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quoteHistory(descriptor.clazz))
                .append(" where id = ").append(id)
                .append(" order by time");
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        for (Map<String, Object> record : records) {
            entities.add(toEntity(record, descriptor));
        }
        E entity = load(clazz, id);
        if (entity != null) {
            entities.add(entity);
        }
        if (entities.isEmpty()) {
            throw new ManagerException("entity not found");
        }
        return entities;
    }

    public <E extends Entity> E restore(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quoteHistory(clazz))
                .append(" where id = ").append(id)
                .append(" and status = ").append(Status.REMOVED.ordinal())
                .append(" and not exists (select id from ").append(quote(clazz)).append(" where id = ").append(id).append(")")
                .append(" order by time desc limit 1");
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        if (records.isEmpty()) {
            throw new ManagerException("entity not found or not removed");
        }
        E previous = toEntity(records.get(0), descriptor);
        previous.setStatus(Status.VALID);
        previous.setTime(now());
        executeTriggers(Event.PRE_RESTORE, null, previous);
        preCheck(previous);
        sqlInsert(previous);
        executeTriggers(Event.POST_RESTORE, null, previous);
        return cache.put(previous);
    }

    public void freeCache() {
        cache.clear();
    }

    public void freeCache(Entity entity) {
        cache.remove(entity);
    }

    private <E extends Entity> void sqlInsert(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) entity.getClass());
        StringBuilder sql = new StringBuilder();
        StringBuilder values = new StringBuilder();
        sql.append("insert into ").append(quote(descriptor.clazz))
                .append(" (id, status, time");
        values.append(entity.getId()).append(", ")
                .append(entity.getStatus().ordinal()).append(", '")
                .append(entity.getTime().format(DATETIME_FORMATTER)).append("'");
        Object value;
        for (FieldDescriptor field : descriptor.fields.values()) {
            value = field.getter.invoke(entity);
            if (value != null) {
                sql.append(", ").append(quote(field.sqlField));
                values.append(", ").append(sqlValue(value));
            }
        }
        sql.append(") values (").append(values).append(")");
        try {
            databaseManager.update(sql.toString());
        } catch (SQLIntegrityConstraintViolationException ce) {
            integrityConstraintCheck(entity, ce);
        }
    }

    private <E extends Entity> void sqlHistory(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) entity.getClass());
        StringBuilder sql = new StringBuilder();
        StringBuilder values = new StringBuilder();
        sql.append("insert into ").append(quoteHistory(descriptor.clazz))
                .append(" (id, status, time");
        values.append(entity.getId()).append(", ")
                .append(entity.getStatus().ordinal()).append(", '")
                .append(entity.getTime().format(DATETIME_FORMATTER)).append("'");
        Object value;
        for (FieldDescriptor field : descriptor.fields.values()) {
            value = field.getter.invoke(entity);
            if (value != null) {
                sql.append(", ").append(quote(field.sqlField));
                values.append(", ").append(sqlValue(value));
            }
        }
        sql.append(") values (").append(values).append(")");
        databaseManager.update(sql.toString());
    }

    private <E extends Entity> void sqlUpdate(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) entity.getClass());
        StringBuilder sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.clazz))
                .append(" set time = '").append(entity.getTime().format(DATETIME_FORMATTER)).append("'");
        Object value;
        for (FieldDescriptor field : descriptor.fields.values()) {
            value = field.getter.invoke(entity);
            sql.append(", ").append(quote(field.sqlField)).append(" = ").append(sqlValue(value));
        }
        sql.append(" where id = ").append(entity.getId());
        int update = 0;
        try {
            update = databaseManager.update(sql.toString());
        } catch (SQLIntegrityConstraintViolationException ce) {
            integrityConstraintCheck(entity, ce);
        }
        if (update != 1) {
            throw new SQLException("entity not found or removed");
        }
    }

    private <E extends Entity> void sqlDelete(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) entity.getClass());
        StringBuilder sql = new StringBuilder();
        sql.append("delete from ").append(quote(descriptor.clazz))
                .append(" where id = ").append(entity.getId());
        int update = 0;
        try {
            update = databaseManager.update(sql.toString());
        } catch (SQLIntegrityConstraintViolationException ce) {
            deleteCheck(entity, ce);
        }
        if (update != 1) {
            throw new SQLException("entity not found or removed");
        }
    }

    private <E extends Entity> void preCheck(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        preCheck(null, entity);
    }

    private <E extends Entity> void preCheck(E previous, E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) entity.getClass());
        Object previousValue, value;
        String pattern;
        for (FieldDescriptor field : descriptor.fields.values()) {
            value = field.getter.invoke(entity);
            previousValue = previous == null ? null : field.getter.invoke(previous);
            if (field.getter.isAnnotationPresent(Required.class) && value == null) {
                throw new ManagerException(field.name + " required");
            }
            if (field.getter.isAnnotationPresent(Final.class) && previousValue != null && !previousValue.equals(value)) {
                throw new ManagerException(field.name + " final");
            }
            if (field.getter.isAnnotationPresent(Matched.class) && value != null) {
                pattern = (field.getter.getAnnotation(Matched.class)).value();
                if (!value.toString().matches(pattern)) {
                    throw new ManagerException(field.name + " not valid");
                }
            }
        }
        for (FieldDescriptor field : descriptor.fields.values()) {
            if (field.getter.isAnnotationPresent(Unique.class)) {
                uniqueCheck(entity, field);
            }
        }
        executeChecks(previous, entity);
    }

    private <E extends Entity> void integrityConstraintCheck(E entity, SQLIntegrityConstraintViolationException ex) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) entity.getClass());
        for (FieldDescriptor field : descriptor.fields.values()) {
            if (field.getter.isAnnotationPresent(Unique.class)) {
                uniqueCheck(entity, field);
            }
            if (Entity.class.isAssignableFrom(field.type)) {
                referenceCheck(entity, field);
            }
        }
        throw ex;
    }

    private <E extends Entity> void referenceCheck(E entity, FieldDescriptor field) throws SQLException, ReflectiveOperationException, ManagerException {
        Object value = field.getter.invoke(entity);
        if (value != null) {
            StringBuilder sql = new StringBuilder();
            sql.append("select count(id) from ").append(quote(field.type))
                    .append(" where id = ").append(sqlValue(value));
            if (databaseManager.queryFirst(sql.toString(), Number.class).intValue() == 0) {
                throw new ManagerException(field.name + " not found");
            }
        }
    }

    private <E extends Entity> void uniqueCheck(E entity, FieldDescriptor field) throws SQLException, ReflectiveOperationException, ManagerException {
        Object value = field.getter.invoke(entity);
        if (value != null) {
            Descriptor<E> descriptor = descriptors.get((Class<E>) entity.getClass());
            StringBuilder sql = new StringBuilder();
            sql.append("select count(id) from ").append(quote(descriptor.clazz))
                    .append(" where ").append(quote(field.sqlField)).append(" = ").append(sqlValue(value))
                    .append(" and id <> ").append(entity.getId());
            for (String group : field.getter.getAnnotation(Unique.class).group()) {
                FieldDescriptor groupField = descriptor.fields.get(group);
                if (groupField == null) {
                    throw new ManagerException("group unique field " + group + " not found");
                }
                if (!groupField.getter.isAnnotationPresent(Required.class)) {
                    throw new ManagerException("group unique field " + groupField.name + " must be required");
                }
                Object groupValue = groupField.getter.invoke(entity);
                if (groupValue != null) {
                    sql.append(" and ").append(quote(groupField.sqlField)).append(" = ").append(sqlValue(groupValue));
                }
            }
            if (databaseManager.queryFirst(sql.toString(), Number.class).intValue() > 0) {
                throw new ManagerException(field.name + " not unique");
            }
        }
    }

    private void deleteCheck(Entity entity, SQLIntegrityConstraintViolationException ex) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql;
        for (Descriptor<? extends Entity> descriptor : descriptors.values()) {
            for (FieldDescriptor field : descriptor.fields.values()) {
                if (entity.getClass().isAssignableFrom(field.type)) {
                    sql = new StringBuilder();
                    sql.append("select count(").append(quote(field.sqlField)).append(") from ").append(quote(descriptor.clazz))
                            .append(" where ").append(quote(field.sqlField)).append(" = ").append(entity.getId());
                    if (databaseManager.queryFirst(sql.toString(), Number.class).intValue() > 0) {
                        throw new ManagerException("referenced by " + descriptor.name);
                    }
                }
            }
        }
        throw ex;
    }

    private <E extends Entity> void changesCheck(E previous, E entity) throws ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) previous.getClass());
        Object previousValue, newValue;
        for (FieldDescriptor field : descriptor.fields.values()) {
            previousValue = field.getter.invoke(previous);
            newValue = field.getter.invoke(entity);
            if (!Objects.equals(previousValue, newValue)) {
                return;
            }
        }
        throw new ManagerException("no changes");
    }

    public synchronized long newId() throws SQLException {
        return newId(1);
    }

    public synchronized long newId(int amount) throws SQLException {
        if (amount <= 0) {
            throw new SQLException("invalid amount id number");
        }
        Connection connection = databaseManager.createConnection();
        try {
            String sql = "update _sequence set id = id + " + amount;
            databaseManager.update(sql, connection);
            sql = "select id from _sequence";
            long id = databaseManager.queryFirst(sql, Long.class, connection);
            connection.commit();
            return id - amount;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (Exception s) {
            }
            throw e;
        } finally {
            try {
                connection.close();
            } catch (Exception s) {
            }
        }
    }

    private <E extends Entity> E getPrevious(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException {
        E previous = cache.getPreviousClone(clazz, id);
        if (previous != null) {
            return previous;
        } else {
            Descriptor<E> descriptor = descriptors.get(clazz);
            StringBuilder sql = new StringBuilder();
            sql.append("select * from ").append(quote(descriptor.clazz))
                    .append(" where id = ").append(id);
            List<Map<String, Object>> records = databaseManager.query(sql.toString());
            if (records.isEmpty()) {
                return null;
            }
            return toEntity(records.get(0), descriptor);
        }
    }

    private <E extends Entity> E merge(E previous, Map<String, Object> values) throws ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) previous.getClass());
        E entity = descriptor.constructor.newInstance();
        entity.setId(previous.getId());
        Object value;
        for (FieldDescriptor field : descriptor.fields.values()) {
            if (values.containsKey(field.name)) {
                try {
                    value = mapJsonToEntity(field, values.remove(field.name));
                } catch (ClassCastException | DateTimeParseException e) {
                    throw new ManagerException("invalid field " + field.name);
                }
            } else {
                value = field.getter.invoke(previous);
            }
            field.setter.invoke(entity, value);
        }
        for (String field : values.keySet()) {
            throw new ManagerException("invalid field " + field);
        }
        return entity;
    }

    private <E extends Entity> E clone(E previous) throws ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) previous.getClass());
        E entity = descriptor.constructor.newInstance();
        entity.setId(previous.getId());
        entity.setStatus(previous.getStatus());
        entity.setTime(previous.getTime());
        for (FieldDescriptor field : descriptor.fields.values()) {
            Object value = field.getter.invoke(previous);
            field.setter.invoke(entity, value);
        }
        return entity;
    }

    private <E extends Entity> E toEntity(Map<String, Object> record, Descriptor<E> descriptor) throws ReflectiveOperationException {
        E entity = descriptor.constructor.newInstance();
        entity.setId(((Number) record.get("id")).longValue());
        entity.setStatus(Status.values()[((Number) record.get("status")).intValue()]);
        entity.setTime((LocalDateTime) record.get("time"));
        Object value;
        for (FieldDescriptor field : descriptor.fields.values()) {
            value = record.get(field.sqlField);
            field.setter.invoke(entity, mapSqlToEntity(field, value));
        }
        return entity;
    }

    private <E extends Entity> Map<String, Object> toMap(E entity, Descriptor<E> descriptor) throws ReflectiveOperationException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("status", entity.getStatus());
        map.put("time", entity.getTime());
        Object value;
        for (FieldDescriptor field : descriptor.fields.values()) {
            value = field.getter.invoke(entity);
            if (value instanceof Entity subEntity) {
                map.put(field.name, Map.of("id", subEntity.getId()));
            } else {
                map.put(field.name, value);
            }
        }
        return map;
    }

    public <E extends Entity> Map<String, Object> toMap(E entity) throws ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get(entity.getClass());
        return toMap(entity, descriptor);
    }

    public <E extends Entity> List<Map<String, Object>> toMap(List<E> entities) throws ReflectiveOperationException {
        if (entities.isEmpty()) {
            return Collections.<Map<String, Object>>emptyList();
        }
        List<Map<String, Object>> list = new ArrayList<>(entities.size());
        Descriptor<E> descriptor = descriptors.get(entities.get(0).getClass());
        for (E entity : entities) {
            list.add(toMap(entity, descriptor));
        }
        return list;
    }

    static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    private class Descriptor<E extends Entity> {

        private final Class clazz;
        private final String name;
        private final Constructor<E> constructor;
        private final Map<String, FieldDescriptor> fields = new LinkedHashMap<>();
        private final List<CheckInfo<E>> checks = new ArrayList<>();
        private final Map<Event, List<TriggerInfo<E>>> triggers = new LinkedHashMap<>();

        private Descriptor(Class<E> clazz) throws ReflectiveOperationException {
            this.clazz = clazz;
            name = clazz.getSimpleName();
            constructor = clazz.getConstructor();
            FieldDescriptor fieldDescriptor;
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    fieldDescriptor = new FieldDescriptor();
                    fieldDescriptor.type = field.getType();
                    fieldDescriptor.name = field.getName();
                    fieldDescriptor.sqlField = field.getName();
                    if (Entity.class.isAssignableFrom(field.getType())) {
                        fieldDescriptor.sqlField = fieldDescriptor.sqlField.concat("_id");
                    }
                    fieldDescriptor.getter = clazz.getDeclaredMethod("get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1));
                    fieldDescriptor.setter = clazz.getDeclaredMethod("set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1), field.getType());
                    fields.put(fieldDescriptor.name, fieldDescriptor);
                }
            }
        }
    }

    private class FieldDescriptor {

        private Class type;
        private String name;
        private String sqlField;
        private Method getter;
        private Method setter;
    }

    public static String quote(String name) {
        return "`" + name + "`";
    }

    public static String quote(Class<? extends Entity> clazz) {
        return "`" + clazz.getSimpleName() + "`";
    }

    public static String quoteHistory(Class<? extends Entity> clazz) {
        return "`" + clazz.getSimpleName() + "_`";
    }

    public static String sqlValue(Object value) throws ReflectiveOperationException {
        if (value == null) {
            return "null";
        } else if (value instanceof String string) {
            return "'" + string.replaceAll("'", "''") + "'";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof LocalDate localDate) {
            return "'" + localDate.format(DATE_FORMATTER) + "'";
        } else if (value instanceof LocalDateTime localDateTime) {
            return "'" + localDateTime.format(DATETIME_FORMATTER) + "'";
        } else if (value instanceof Entity entity) {
            return entity.getId().toString();
        } else if (value instanceof Enum enumeration) {
            return "'" + enumeration.name() + "'";
        } else {
            throw new ReflectiveOperationException("unsupported type " + value.getClass());
        }
    }

    private <E extends Entity> Object mapSqlToEntity(FieldDescriptor field, Object value) throws ReflectiveOperationException {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(field.type)) {
            return (String) value;
        } else if (Boolean.class.isAssignableFrom(field.type)) {
            return (Boolean) value;
        } else if (Byte.class.isAssignableFrom(field.type)) {
            return ((Number) value).byteValue();
        } else if (Short.class.isAssignableFrom(field.type)) {
            return ((Number) value).shortValue();
        } else if (Integer.class.isAssignableFrom(field.type)) {
            return ((Number) value).intValue();
        } else if (Long.class.isAssignableFrom(field.type)) {
            return ((Number) value).longValue();
        } else if (Float.class.isAssignableFrom(field.type)) {
            return ((Number) value).floatValue();
        } else if (Double.class.isAssignableFrom(field.type)) {
            return ((Number) value).doubleValue();
        } else if (LocalDate.class.isAssignableFrom(field.type)) {
            return (LocalDate) value;
        } else if (LocalDateTime.class.isAssignableFrom(field.type)) {
            return (LocalDateTime) value;
        } else if (Enum.class.isAssignableFrom(field.type)) {
            return Enum.valueOf(field.type, (String) value);
        } else if (Entity.class.isAssignableFrom(field.type)) {
            Entity reference = (Entity) field.type.getConstructor().newInstance();
            reference.setId(((Number) value).longValue());
            return reference;
        } else {
            throw new ReflectiveOperationException("unsupported type " + value.getClass());
        }
    }

    private <E extends Entity> Object mapJsonToEntity(FieldDescriptor field, Object value) throws ReflectiveOperationException {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(field.type)) {
            return (String) value;
        } else if (Boolean.class.isAssignableFrom(field.type)) {
            return (Boolean) value;
        } else if (Byte.class.isAssignableFrom(field.type)) {
            return ((Number) value).byteValue();
        } else if (Short.class.isAssignableFrom(field.type)) {
            return ((Number) value).shortValue();
        } else if (Integer.class.isAssignableFrom(field.type)) {
            return ((Number) value).intValue();
        } else if (Long.class.isAssignableFrom(field.type)) {
            return ((Number) value).longValue();
        } else if (Float.class.isAssignableFrom(field.type)) {
            return ((Number) value).floatValue();
        } else if (Double.class.isAssignableFrom(field.type)) {
            return ((Number) value).doubleValue();
        } else if (LocalDate.class.isAssignableFrom(field.type)) {
            return LocalDate.parse((String) value);
        } else if (LocalDateTime.class.isAssignableFrom(field.type)) {
            return LocalDateTime.parse((String) value);
        } else if (Enum.class.isAssignableFrom(field.type)) {
            return Enum.valueOf(field.type, (String) value);
        } else if (Entity.class.isAssignableFrom(field.type)) {
            Long id = ((Number) ((Map) value).get("id")).longValue();
            Entity entity = (Entity) field.type.getConstructor().newInstance();
            entity.setId(id);
            return entity;
        } else {
            throw new ReflectiveOperationException("unsupported type " + value.getClass());
        }
    }

    private class CheckInfo<E extends Entity> implements Comparable<CheckInfo> {

        private final Object object;
        private final Method method;
        private final Integer order;

        public CheckInfo(Object object, Method method, Integer order) {
            this.object = object;
            this.method = method;
            this.order = order;
        }

        @Override
        public int compareTo(CheckInfo other) {
            return this.order.compareTo(other.order);
        }

    }

    <E extends Entity> void addCheck(Class<E> clazz, Check annotation, Object object, Method method) throws ReflectiveOperationException {
        checkParameters(clazz, method, clazz, clazz);
        Descriptor<E> descriptor = descriptors.get(clazz);
        descriptor.checks.add(new CheckInfo<>(object, method, annotation.order()));
        Collections.sort(descriptor.checks);
    }

    private <E extends Entity> void executeChecks(E previous, E entity) throws SQLException, ManagerException, ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) (entity != null ? entity.getClass() : previous.getClass()));
        for (CheckInfo<E> check : descriptor.checks) {
            try {
                check.method.invoke(check.object, previous, entity);
            } catch (InvocationTargetException ex) {
                rethrow(ex);
            }
        }
    }

    private class TriggerInfo<E extends Entity> implements Comparable<TriggerInfo> {

        private final Object object;
        private final Method method;
        private final Integer order;

        public TriggerInfo(Object object, Method method, Integer order) {
            this.object = object;
            this.method = method;
            this.order = order;
        }

        @Override
        public int compareTo(TriggerInfo other) {
            return this.order.compareTo(other.order);
        }

    }

    <E extends Entity> void addTrigger(Class<E> clazz, Trigger annotation, Object object, Method method) throws ReflectiveOperationException {
        if (method.getParameterCount() == 3) {
            checkParameters(clazz, method, clazz, clazz, Event.class);
        } else {
            checkParameters(clazz, method, clazz, clazz);
        }
        Descriptor<E> descriptor = descriptors.get(clazz);
        List<TriggerInfo<E>> triggers = descriptor.triggers.get(annotation.event());
        if (triggers == null) {
            triggers = new ArrayList<>();
            descriptor.triggers.put(annotation.event(), triggers);
        }
        triggers.add(new TriggerInfo<>(object, method, annotation.order()));
        Collections.sort(triggers);
    }

    private <E extends Entity> void executeTriggers(Event event, E previous, E entity) throws SQLException, ManagerException, ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get((Class<E>) (entity != null ? entity.getClass() : previous.getClass()));
        List<TriggerInfo<E>> triggers = descriptor.triggers.get(event);
        if (triggers != null) {
            for (TriggerInfo<E> trigger : triggers) {
                try {
                    if (trigger.method.getParameterCount() == 3) {
                        trigger.method.invoke(trigger.object, previous, entity, event);
                    } else {
                        trigger.method.invoke(trigger.object, previous, entity);
                    }
                } catch (InvocationTargetException ex) {
                    rethrow(ex);
                }
            }
        }
    }

    private void checkParameters(Class<? extends Entity> clazz, Method method, Class<?>... expected) throws ReflectiveOperationException {
        if (!descriptors.containsKey(clazz)) {
            throw new ReflectiveOperationException("entity " + clazz.getSimpleName() + " not registered");
        }
        Class<?>[] parameters = method.getParameterTypes();
        boolean valid = parameters.length == expected.length;
        for (int i = 0; valid && i < parameters.length; i++) {
            valid = parameters[i].isAssignableFrom(expected[i]);
        }
        if (!valid) {
            throw new ReflectiveOperationException("invalid signature " + method.getDeclaringClass().getSimpleName() + "." + method.getName());
        }
    }

    private static void rethrow(InvocationTargetException ex) throws SQLException, ManagerException, ReflectiveOperationException {
        Throwable cause = ex.getCause();
        if (cause instanceof SQLException c) {
            throw c;
        }
        if (cause instanceof ManagerException c) {
            throw c;
        }
        if (cause instanceof ReflectiveOperationException c) {
            throw c;
        }
        if (cause instanceof RuntimeException c) {
            throw c;
        }
        if (cause instanceof Error c) {
            throw c;
        }
        throw ex;
    }

    private class Cache {

        private final ThreadLocal<Map<Class<? extends Entity>, Map<Long, Entity>>> realCache = ThreadLocal.withInitial(HashMap::new);
        private final ThreadLocal<Map<Class<? extends Entity>, Map<Long, Entity>>> prevCache = ThreadLocal.withInitial(HashMap::new);

        private <E extends Entity> E put(E entity) throws ReflectiveOperationException {
            realCache.get().computeIfAbsent(entity.getClass(), k -> new HashMap<>()).put(entity.getId(), entity);
            prevCache.get().computeIfAbsent(entity.getClass(), k -> new HashMap<>()).put(entity.getId(), DomainManager.this.clone(entity));
            return entity;
        }

        private <E extends Entity> List<E> put(List<E> entities) throws ReflectiveOperationException {
            if (!entities.isEmpty()) {
                Map<Long, Entity> entityMap = realCache.get().computeIfAbsent(entities.get(0).getClass(), k -> new HashMap<>());
                for (E entity : entities) {
                    entityMap.put(entity.getId(), entity);
                }
                entityMap = prevCache.get().computeIfAbsent(entities.get(0).getClass(), k -> new HashMap<>());
                for (E entity : entities) {
                    entityMap.put(entity.getId(), DomainManager.this.clone(entity));
                }
            }
            return entities;
        }

        private <E extends Entity> E remove(E entity) {
            Map<Long, Entity> entityMap = realCache.get().get(entity.getClass());
            if (entityMap != null) {
                entityMap.remove(entity.getId());
            }
            entityMap = prevCache.get().get(entity.getClass());
            if (entityMap != null) {
                entityMap.remove(entity.getId());
            }
            return entity;
        }

        private <E extends Entity> E get(Class<E> clazz, Long id) {
            Map<Long, Entity> entityMap = realCache.get().get(clazz);
            if (entityMap != null) {
                return (E) entityMap.get(id);
            }
            return null;
        }

        private <E extends Entity> E getPreviousClone(Class<E> clazz, Long id) throws ReflectiveOperationException {
            Map<Long, Entity> entityMap = prevCache.get().get(clazz);
            if (entityMap != null) {
                E entity = (E) entityMap.get(id);
                return entity == null ? entity : DomainManager.this.clone(entity);
            }
            return null;
        }

        private void clear() {
            realCache.remove();
            prevCache.remove();
        }
    }

}
