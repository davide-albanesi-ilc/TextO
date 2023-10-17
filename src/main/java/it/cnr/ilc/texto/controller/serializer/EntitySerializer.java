package it.cnr.ilc.texto.controller.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Status;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 *
 * @author oakgen
 */
public class EntitySerializer extends StdSerializer<Entity> {

    public EntitySerializer() {
        super(Entity.class);
    }

    @Override
    public void serialize(Entity entity, JsonGenerator generator, SerializerProvider serializer) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField("id", entity.getId());
        if (entity.getStatus().equals(Status.VALID)) {
            String field;
            Object value;
            for (Method method : Entity.getters(entity.getClass())) {
                try {
                    field = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                    value = method.invoke(entity);
                    generator.writeObjectField(field, value);
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        generator.writeEndObject();
    }

}
