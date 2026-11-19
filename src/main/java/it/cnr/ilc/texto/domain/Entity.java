package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.controller.serializer.EntitySerializer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author oakgen
 */
@JsonSerialize(using = EntitySerializer.class)
public abstract class Entity {

    private Long id;
    private Status status = Status.GHOST;
    private LocalDateTime time;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status == null ? Status.GHOST : status;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + id;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Entity entity && this.id.equals(entity.id);
    }

    public static <E extends Entity> E newGhost(Class<E> clazz, Long id) throws ReflectiveOperationException {
        E entity = clazz.getConstructor().newInstance();
        entity.setId(id);
        return entity;
    }

    public static List<Method> getters(Class<? extends Entity> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Method> getters = new ArrayList<>(fields.length);
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                try {
                    getters.add(clazz.getDeclaredMethod("get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1)));
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return getters;
    }

}
