package it.cnr.ilc.texto.controller.serializer;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Status;
import java.lang.reflect.Method;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 *
 * @author oakgen
 */
public class EntitySerializer extends StdSerializer<Entity> {

    public EntitySerializer() {
        super(Entity.class);
    }

    @Override
    public void serialize(Entity entity, JsonGenerator generator, SerializationContext provider) throws JacksonException {
        generator.writeStartObject();
        generator.writeNumberProperty("id", entity.getId());
        if (entity.getStatus().equals(Status.VALID)) {
            String field;
            Object value;
            for (Method method : Entity.getters(entity.getClass())) {
                try {
                    field = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                    value = method.invoke(entity);
                    generator.writePOJOProperty(field, value);
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        generator.writeEndObject();
    }

}
