package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Test;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.exception.ManagerException;
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
    public String getLog(Test test) {
        return test.getStringt();
    }

    @Check
    public void checking(Test previous, Test entity) throws ManagerException, SQLException {
        if (Integer.valueOf(-1).equals(entity.getIntegert())) {
            throw new ManagerException("invalid integert");
        }
        if (Integer.valueOf(-2).equals(entity.getIntegert())) {
            throw new SQLException("sql error");
        }
    }

}
