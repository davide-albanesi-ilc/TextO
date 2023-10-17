package it.cnr.ilc.texto.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.cnr.ilc.texto.controller.serializer.EntitySerializer;
import it.cnr.ilc.texto.domain.annotation.Ignore;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("get") && !m.isAnnotationPresent(Ignore.class))
                .collect(Collectors.toList());
    }

}
