package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.manager.EntityManager;
import it.cnr.ilc.texto.manager.RowManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("row")
public class RowController extends EntityController<Row> {

    @Autowired
    private RowManager rowManager;

    @Override
    protected EntityManager<Row> entityManager() {
        return rowManager;
    }

    @Override
    protected Class<Row> entityClass() {
        return Row.class;
    }

    @Override
    protected void checkAccess(Row row, Action action) throws ForbiddenException, ReflectiveOperationException, SQLException, ManagerException {
        if (row.getResource() != null) {
            accessManager.checkAccess(row.getResource(), action.equals(Action.READ) ? Action.READ : Action.WRITE);
        }
    }

}
