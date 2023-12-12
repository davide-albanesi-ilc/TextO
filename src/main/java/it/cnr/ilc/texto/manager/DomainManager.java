package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Status;
import it.cnr.ilc.texto.domain.annotation.Final;
import it.cnr.ilc.texto.domain.annotation.Matched;
import it.cnr.ilc.texto.domain.annotation.Required;
import it.cnr.ilc.texto.domain.annotation.Unique;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DomainManager extends Manager {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    protected DatabaseManager databaseManager;

    private final Map<Class, Descriptor> descriptors = new HashMap<>();
    private final Cache cache = new Cache();

    private <E extends Entity> Descriptor<E> getDesriptor(Class<E> clazz) throws ReflectiveOperationException {
        Descriptor<E> descriptor = descriptors.get(clazz);
        if (descriptor == null) {
            descriptor = new Descriptor<>(clazz);
            descriptors.put(clazz, descriptor);
        }
        return descriptor;
    }

    public <E extends Entity> List<E> load(Class<E> clazz) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(descriptor.name))
                .append(" where status = ").append(Status.VALID.ordinal());
        List<E> entities = new ArrayList<>();
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        for (Map<String, Object> record : records) {
            entities.add(toEntity(record, descriptor));
        }
        return cache.put(entities);
    }

    public <E extends Entity> E load(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        E entity = cache.get(clazz, id);
        if (entity != null) {
            return entity;
        }
        Descriptor<E> descriptor = getDesriptor(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(descriptor.name))
                .append(" where id = ").append(id)
                .append(" and status = ").append(Status.VALID.ordinal());
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        if (records.isEmpty()) {
            return null;
        } else if (records.size() > 1) {
            throw new SQLException("not unique query");
        }
        entity = toEntity(records.get(0), descriptor);
        return cache.put(entity);
    }

    public <E extends Entity> List<E> load(Class<E> clazz, String sql) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        List<E> entities = new ArrayList<>();
        List<Map<String, Object>> records = databaseManager.query(sql);
        for (Map<String, Object> record : records) {
            entities.add(toEntity(record, descriptor));
        }
        return cache.put(entities);
    }

    public <E extends Entity> E loadUnique(Class<E> clazz, String sql) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
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
        Descriptor<E> descriptor = getDesriptor(clazz);
        E entity = descriptor.constructor.newInstance();
        entity.setId(newId());
        entity.setStatus(Status.VALID);
        entity.setTime(LocalDateTime.now());
        preCheck(entity);
        sqlInsert(entity);
        return cache.put(entity);
    }

    public <E extends Entity> void create(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        entity.setId(newId());
        entity.setStatus(Status.VALID);
        entity.setTime(LocalDateTime.now());
        preCheck(entity);
        sqlInsert(entity);
        cache.put(entity);
    }

    public <E extends Entity> void update(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) entity.getClass());
        E previous = (E) load(entity.getClass(), entity.getId());
        if (previous == null) {
            throw new ManagerException("entity not found or removed");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.name))
                .append(" set status = ").append(Status.HISTORY.ordinal())
                .append(" where id = ").append(entity.getId())
                .append(" and status = ").append(Status.VALID.ordinal());
        if (databaseManager.update(sql.toString()) != 1) {
            throw new ManagerException("entity not found or removed");
        }
        entity.setStatus(Status.VALID);
        entity.setTime(LocalDateTime.now());
        checkChanges(previous, entity);
        preCheck(previous, entity);
        sqlInsert(entity);
        cache.put(entity);
    }

    public <E extends Entity> void update(E previous, E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) previous.getClass());
        StringBuilder sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.name))
                .append(" set status = ").append(Status.HISTORY.ordinal())
                .append(" where id = ").append(previous.getId())
                .append(" and status = ").append(Status.VALID.ordinal());
        if (databaseManager.update(sql.toString()) != 1) {
            throw new ManagerException("entity not found or removed");
        }
        entity.setId(previous.getId());
        entity.setStatus(Status.VALID);
        entity.setTime(LocalDateTime.now());
        merge(previous, entity);
        checkChanges(previous, entity);
        preCheck(previous, entity);
        sqlInsert(entity);
        cache.put(entity);
    }

    public <E extends Entity> E update(Class<E> clazz, Long id, E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        E previous = load(clazz, id);
        if (previous == null) {
            throw new ManagerException("entity not found or removed");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.name))
                .append(" set status = ").append(Status.HISTORY.ordinal())
                .append(" where id = ").append(id)
                .append(" and status = ").append(Status.VALID.ordinal());
        if (databaseManager.update(sql.toString()) != 1) {
            throw new ManagerException("entity not found or removed");
        }
        entity.setId(previous.getId());
        entity.setStatus(Status.VALID);
        entity.setTime(LocalDateTime.now());
        merge(previous, entity);
        checkChanges(previous, entity);
        preCheck(previous, entity);
        sqlInsert(entity);
        return cache.put(entity);
    }

    public <E extends Entity> void remove(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) entity.getClass());
        E previous = (E) load(entity.getClass(), entity.getId());
        if (previous == null) {
            throw new ManagerException("entity not found or removed");
        }
        referenceCheck(entity);
        StringBuilder sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.name))
                .append(" set status = ").append(Status.HISTORY.ordinal())
                .append(" where id = ").append(entity.getId())
                .append(" and status = ").append(Status.VALID.ordinal());
        if (databaseManager.update(sql.toString()) != 1) {
            throw new ManagerException("entity not found or removed");
        }
        entity.setStatus(Status.REMOVED);
        entity.setTime(LocalDateTime.now());
        preCheck(previous, entity);
        sqlInsert(entity);
        cache.remove(entity);
    }

    public <E extends Entity> E remove(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        E entity = load(clazz, id);
        if (entity == null) {
            throw new ManagerException("entity not found or removed");
        }
        referenceCheck(entity);
        StringBuilder sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.name))
                .append(" set status = ").append(Status.HISTORY.ordinal())
                .append(" where id = ").append(entity.getId())
                .append(" and status = ").append(Status.VALID.ordinal());
        databaseManager.update(sql.toString());
        entity.setStatus(Status.REMOVED);
        entity.setTime(LocalDateTime.now());
        sqlInsert(entity);
        return cache.remove(entity);
    }

    public <E extends Entity> List<E> history(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(descriptor.name))
                .append(" where id = ").append(id)
                .append(" order  by time desc");
        List<E> entities = new ArrayList<>();
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        for (Map<String, Object> record : records) {
            entities.add(toEntity(record, descriptor));
        }
        return entities;
    }

    public <E extends Entity> void restore(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) entity.getClass());
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(descriptor.name))
                .append(" where id = ").append(entity.getId())
                .append(" and status = ").append(Status.REMOVED.ordinal());
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        if (records.isEmpty()) {
            throw new ManagerException("entity not found or not removed");
        } else if (records.size() > 1) {
            throw new SQLException("not unique query");
        }
        E previous = toEntity(records.get(0), descriptor);
        sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.name))
                .append(" set status = ").append(Status.HISTORY.ordinal())
                .append(" where id = ").append(entity.getId())
                .append(" and status = ").append(Status.REMOVED.ordinal());
        databaseManager.update(sql.toString());
        entity.setStatus(Status.VALID);
        entity.setTime(LocalDateTime.now());
        preCheck(previous, entity);
        sqlInsert(entity);
        cache.put(entity);
    }

    public <E extends Entity> E restore(Class<E> clazz, Long id) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(descriptor.name))
                .append(" where id = ").append(id)
                .append(" and status = ").append(Status.REMOVED.ordinal());
        List<Map<String, Object>> records = databaseManager.query(sql.toString());
        if (records.isEmpty()) {
            throw new ManagerException("entity not found or not removed");
        } else if (records.size() > 1) {
            throw new SQLException("not unique query");
        }
        E entity = toEntity(records.get(0), descriptor);
        sql = new StringBuilder();
        sql.append("update ").append(quote(descriptor.name))
                .append(" set status = ").append(Status.HISTORY.ordinal())
                .append(" where id = ").append(entity.getId())
                .append(" and status = ").append(Status.REMOVED.ordinal());
        databaseManager.update(sql.toString());
        entity.setStatus(Status.VALID);
        entity.setTime(LocalDateTime.now());
        sqlInsert(entity);
        return cache.put(entity);
    }

    public void freeCache() {
        cache.clear();
    }

    private <E extends Entity> void sqlInsert(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) entity.getClass());
        StringBuilder sql = new StringBuilder();
        StringBuilder values = new StringBuilder();
        sql.append("insert into ").append(quote(descriptor.name))
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
        } catch (SQLIntegrityConstraintViolationException e) {
            postCheck(entity);
        }
    }

    private <E extends Entity> void preCheck(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        preCheck(null, entity);
    }

    private <E extends Entity> void preCheck(E previous, E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) entity.getClass());
        StringBuilder sql;
        Object previousValue, value;
        FieldDescriptor groupField;
        Object groupValue;
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
            if (field.getter.isAnnotationPresent(Unique.class) && value != null) {
                sql = new StringBuilder();
                sql.append("select count(id) from ").append(quote(descriptor.name))
                        .append(" where status = 1 and ")
                        .append(quote(field.sqlField)).append(" = ").append(sqlValue(value));
                if (entity.getId() != null) {
                    sql.append(" and id <> ").append(entity.getId());
                }
                for (String group : field.getter.getAnnotation(Unique.class).group()) {
                    groupField = descriptor.fields.get(group);
                    if (groupField == null) {
                        throw new ManagerException("group unique field " + group + " not found");
                    }
                    if (!groupField.getter.isAnnotationPresent(Required.class)) {
                        throw new ManagerException("group unique field " + groupField.name + " must be required");
                    }
                    groupValue = groupField.getter.invoke(entity);
                    if (groupValue != null) {
                        sql.append(" and ").append(quote(groupField.sqlField)).append(" = ").append(sqlValue(groupValue));
                    }
                }
                if (databaseManager.queryFirst(sql.toString(), Number.class).intValue() > 0) {
                    throw new ManagerException(field.name + " not unique");
                }
            }
        }
    }

    private <E extends Entity> void postCheck(E entity) throws SQLException, ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) entity.getClass());
        StringBuilder sql;
        Object value;
        for (FieldDescriptor field : descriptor.fields.values()) {
            if (Entity.class.isAssignableFrom(field.type)) {
                value = field.getter.invoke(entity);
                if (value != null) {
                    sql = new StringBuilder();
                    sql.append("select count(id) from ").append(quote(field.type))
                            .append(" where status = ").append(Status.VALID.ordinal())
                            .append(" and id = ").append(sqlValue(value));
                    if (databaseManager.queryFirst(sql.toString(), Number.class).intValue() == 0) {
                        throw new ManagerException(field.name + " not found");
                    }
                }
            }
        }
    }

    private void referenceCheck(Entity entity) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql;
        for (Descriptor<? extends Entity> descriptor : descriptors.values()) {
            for (FieldDescriptor field : descriptor.fields.values()) {
                if (entity.getClass().isAssignableFrom(field.type)) {
                    sql = new StringBuilder();
                    sql.append("select count(").append(quote(field.sqlField)).append(") from ").append(quote(descriptor.name))
                            .append(" where status = ").append(Status.VALID.ordinal())
                            .append(" and ").append(quote(field.sqlField)).append(" = ").append(entity.getId());
                    if (databaseManager.queryFirst(sql.toString(), Number.class).intValue() > 0) {
                        throw new ManagerException("referenced by " + descriptor.name);
                    }
                }
            }
        }
    }

    private <E extends Entity> void merge(E previous, E entity) throws ReflectiveOperationException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) previous.getClass());
        Object currValue, newValue;
        for (FieldDescriptor field : descriptor.fields.values()) {
            currValue = field.getter.invoke(previous);
            newValue = field.getter.invoke(entity);
            if (newValue == null) {
                field.setter.invoke(entity, currValue);
            }
        }
    }

    private <E extends Entity> void checkChanges(E previous, E entity) throws ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor((Class<E>) previous.getClass());
        Object currValue, newValue;
        for (FieldDescriptor field : descriptor.fields.values()) {
            currValue = field.getter.invoke(previous);
            newValue = field.getter.invoke(entity);
            if (!Objects.equals(currValue, newValue)) {
                return;
            }
        }
        throw new ManagerException("no changes");
    }

    public synchronized long newId() throws SQLException {
        Connection connection = databaseManager.createConnection();
        try {
            String sql = "update _sequence set id = id + 1";
            databaseManager.update(sql, connection);
            sql = "select id from _sequence";
            long id = databaseManager.queryFirst(sql, Long.class, connection);
            connection.commit();
            return id;
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

    private <E extends Entity> E toEntity(Map<String, Object> record, Descriptor<E> descriptor) throws ReflectiveOperationException, ManagerException {
        E entity = descriptor.constructor.newInstance();
        entity.setId(((Number) record.remove("id")).longValue());
        entity.setStatus(Status.values()[((Number) record.remove("status")).intValue()]);
        entity.setTime((LocalDateTime) record.remove("time"));
        Object value;
        Entity reference;
        for (FieldDescriptor field : descriptor.fields.values()) {
            value = record.remove(field.sqlField);
            if (value != null) {
                if (Entity.class.isAssignableFrom(field.type)) {
                    reference = (Entity) field.type.getConstructor().newInstance();
                    reference.setId(((Number) value).longValue());
                    value = reference;
                } else if (Enum.class.isAssignableFrom(field.type)) {
                    value = Enum.valueOf(field.type, (String) value);
                }
                field.setter.invoke(entity, value);
            }
        }
        return entity;
    }

    public <E extends Entity> E toEntity(Map<String, Object> record, Class<E> clazz) throws ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        return toEntity(record, descriptor);
    }

    public <E extends Entity> List<E> toEntity(List<Map<String, Object>> records, Class<E> clazz) throws ReflectiveOperationException, ManagerException {
        Descriptor<E> descriptor = getDesriptor(clazz);
        List<E> list = new ArrayList<>();
        for (Map<String, Object> record : records) {
            list.add(toEntity(record, descriptor));
        }
        return list;
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
        Descriptor<E> descriptor = new Descriptor<>((Class<E>) entity.getClass());
        return toMap(entity, descriptor);
    }

    public <E extends Entity> List<Map<String, Object>> toMap(List<E> entities) throws ReflectiveOperationException {
        if (entities.isEmpty()) {
            return Collections.<Map<String, Object>>emptyList();
        }
        List<Map<String, Object>> list = new ArrayList<>(entities.size());
        Descriptor<E> descriptor = new Descriptor<>((Class<E>) entities.get(0).getClass());
        for (E entity : entities) {
            list.add(toMap(entity, descriptor));
        }
        return list;
    }

    private class Descriptor<E extends Entity> {

        private final Class clazz;
        private final String name;
        private final Constructor<E> constructor;
        private final Map<String, FieldDescriptor> fields = new LinkedHashMap<>();

        private Descriptor(Class<E> clazz) throws ReflectiveOperationException {
            this.clazz = clazz;
            name = clazz.getSimpleName();
            constructor = clazz.getConstructor();
            FieldDescriptor fieldDescriptor;
            for (Field field : clazz.getDeclaredFields()) {
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

    private class FieldDescriptor {

        private Class type;
        private String name;
        private String sqlField;
        private Method getter;
        private Method setter;
    }

    public static String quote(Class<? extends Entity> clazz) {
        return "`" + clazz.getSimpleName() + "`";
    }

    public static String quote(String name) {
        return "`" + name + "`";
    }

    public static String sqlValue(Object value) throws ReflectiveOperationException {
        if (value instanceof Entity entity) {
            return entity.getId().toString();
        } else if (value instanceof Enum enumeration) {
            return "'" + enumeration.name() + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof String string) {
            return "'" + string.replaceAll("'", "''") + "'";
        } else if (value instanceof LocalDateTime localDateTime) {
            return "'" + localDateTime.format(DATETIME_FORMATTER) + "'";
        } else {
            throw new ReflectiveOperationException("unsupported type " + value.getClass());
        }
    }

    private class Cache {

        private final Map<Thread, Map<Class<? extends Entity>, Map<Long, Entity>>> threadMap = new HashMap<>();

        private <E extends Entity> E put(E entity) {
            Map<Class<? extends Entity>, Map<Long, Entity>> classMap = threadMap.get(Thread.currentThread());
            if (classMap == null) {
                classMap = new HashMap<>();
                threadMap.put(Thread.currentThread(), classMap);
            }
            Map<Long, Entity> entityMap = classMap.get(entity.getClass());
            if (entityMap == null) {
                entityMap = new HashMap<>();
                classMap.put(entity.getClass(), entityMap);
            }
            entityMap.put(entity.getId(), entity);
            return entity;
        }

        private <E extends Entity> List<E> put(List<E> entities) {
            if (!entities.isEmpty()) {
                Map<Class<? extends Entity>, Map<Long, Entity>> classMap = threadMap.get(Thread.currentThread());
                if (classMap == null) {
                    classMap = new HashMap<>();
                    threadMap.put(Thread.currentThread(), classMap);
                }
                Map<Long, Entity> entityMap = classMap.get(entities.get(0).getClass());
                if (entityMap == null) {
                    entityMap = new HashMap<>();
                    classMap.put(entities.get(0).getClass(), entityMap);
                }
                for (E entity : entities) {
                    entityMap.put(entity.getId(), entity);
                }
            }
            return entities;
        }

        private <E extends Entity> E remove(E entity) {
            Map<Class<? extends Entity>, Map<Long, Entity>> classMap = threadMap.get(Thread.currentThread());
            if (classMap != null) {
                Map<Long, Entity> entityMap = classMap.get(entity.getClass());
                if (entityMap != null) {
                    entityMap.remove(entity.getId());
                }
            }
            return entity;
        }

        private <E extends Entity> E get(Class<E> clazz, Long id) {
            Map<Class<? extends Entity>, Map<Long, Entity>> classMap = threadMap.get(Thread.currentThread());
            if (classMap != null) {
                Map<Long, Entity> entityMap = classMap.get(clazz);
                if (entityMap != null) {
                    return (E) entityMap.get(id);
                }
            }
            return null;
        }

        private void clear() {
            threadMap.remove(Thread.currentThread());
        }
    }
}
