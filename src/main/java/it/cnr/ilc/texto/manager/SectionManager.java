package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Section;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.annotation.Check;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.annotation.Trigger.Event;
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
public class SectionManager extends EntityManager<Section> {

    @Lazy
    @Autowired
    private ResourceManager resourceManager;
    @Lazy
    @Autowired
    private RowManager rowManager;

    @Override
    protected Class<Section> entityClass() {
        return Section.class;
    }

    @Override
    public String getLog(Section section) throws SQLException, ReflectiveOperationException {
        if (section.getResource() == null || section.getTitle() == null) {
            return "" + section.getId();
        } else {
            return resourceManager.getLog(resourceManager.load(section.getResource().getId())) + " " + section.getTitle();
        }
    }

    @Check
    public void empty(Section previous, Section section) throws ManagerException {
        if (section.getStart().equals(section.getEnd())) {
            throw new ManagerException("empty section");
        }
    }

    @Trigger(event = Event.PRE_REMOVE)
    public void removeRows(Section previous, Section section) throws SQLException, ReflectiveOperationException, ManagerException {
        rowManager.remove(section);
    }

    public List<Section> load(Resource resource) throws SQLException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Section.class))
                .append(" where resource_id = ").append(resource.getId())
                .append(" and parent_id is null")
                .append(" order by start");
        return load(sql.toString());
    }

    public List<Section> load(Section section) throws SQLException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Section.class))
                .append(" where parent_id = ").append(section.getId())
                .append(" order by start");
        return load(sql.toString());
    }

    public void remove(Resource resource) throws SQLException, ReflectiveOperationException, ManagerException {
        for (Section section : load(resource)) {
            recursiveRemove(section);
        }
    }

    private void recursiveRemove(Section section) throws SQLException, ReflectiveOperationException, ManagerException {
        for (Section subSection : load(section)) {
            recursiveRemove(subSection);
        }
        remove(section);
    }

}
