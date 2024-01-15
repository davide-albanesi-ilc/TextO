package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Test;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.annotation.Trigger.Event;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class TestManager extends EntityManager<Test> {

    @Override
    protected Class<Test> entityClass() {
        return Test.class;
    }

    @Override
    public String getLog(Test test) {
        return test.getStringt();
    }

    @Check
    public void ezzelo(Test previous, Test entity) throws ManagerException {
        if (Integer.valueOf(0).equals(entity.getIntegert())) {
            throw new ManagerException("ezzelo");
        }
    }

    @Trigger(event = Event.POST_CREATE)
    public void mandorla(Test previous, Test entity) {
        System.out.println("mandorla");
    }

    @Trigger(event = Event.POST_CREATE)
    @Trigger(event = Event.POST_REMOVE)
    public void pinolo(Test previous, Test entity) {
        System.out.println("pinolo");
    }

    @Trigger(event = Event.POST_CREATE)
    public void nocciola(Test previous, Test entity) {
        System.out.println("nocciola");
    }
}
