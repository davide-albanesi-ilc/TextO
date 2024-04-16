package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Offset;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import static it.cnr.ilc.texto.manager.ResourceManager.checkOffset;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class RowManager extends EntityManager<Row> {

    @Lazy
    @Autowired
    private ResourceManager resourceManager;

    @Override
    protected Class<Row> entityClass() {
        return Row.class;
    }

    @Override
    public String getLog(Row row) throws SQLException, ReflectiveOperationException {
        if (row.getResource() == null || row.getStart() == null || row.getEnd() == null || row.getNumber() == null) {
            return "" + row.getId();
        } else {
            return resourceManager.getLog(resourceManager.load(row.getResource().getId())) + " "
                    + row.getNumber() + " [" + row.getStart() + "-" + row.getEnd() + "]";
        }
    }

    @Check
    public void empty(Row previous, Row row) throws ManagerException {
        if (row.getStart().equals(row.getEnd())) {
            throw new ManagerException("empty row");
        }
    }

    public List<Row> load(Resource resource, Offset offset) throws SQLException, ManagerException, ReflectiveOperationException {
        checkOffset(offset, resourceManager.getCharacterCount(resource));
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Row.class))
                .append(" where status = 1")
                .append(" and resource_id = ").append(resource.getId())
                .append(" and start >= ").append(offset.start)
                .append(" and end <= ").append(offset.end)
                .append(" order by start");
        return load(sql.toString());
    }

    public Integer rowCount(Resource resource) throws SQLException, ManagerException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select max(number) from ").append(quote(Row.class))
                .append(" where status = 1")
                .append(" and resource_id = ").append(resource.getId());
        return databaseManager.queryFirst(sql.toString(), Number.class).intValue();
    }
}