package it.cnr.ilc.texto.manager.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author oakgen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Triggers.class)
public @interface Trigger {

    public enum Event {
        PRE_CREATE,
        POST_CREATE,
        PRE_UPDATE,
        POST_UPDATE,
        PRE_REMOVE,
        POST_REMOVE,
        PRE_RESTORE,
        POST_RESTORE
    }

    Event event();

    int order() default 0;
}
