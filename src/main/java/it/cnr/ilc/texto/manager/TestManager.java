package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Test;
import java.sql.SQLException;
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
    public String getLog(Test test) throws SQLException, ReflectiveOperationException, ManagerException {
        return test.getRock() + " " + test.getPaper() + " " + test.getScissor();
    }
}
