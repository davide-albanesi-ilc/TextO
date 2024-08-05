package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.domain.SectionType;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class SectionTypeManager extends EntityManager<SectionType> {

    @Override
    protected Class<SectionType> entityClass() {
        return SectionType.class;
    }

    @Override
    public String getLog(SectionType sectionType) {
        return sectionType.getName() != null ? sectionType.getName() : "" + sectionType.getId();
    }

    public List<SectionType> load(Resource resource) throws SQLException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(SectionType.class))
                .append(" where resource_id = ").append(resource.getId())
                .append(" and parent_id is null");
        return load(sql.toString());
    }

    public List<SectionType> load(SectionType sectionType) throws SQLException, ReflectiveOperationException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(quote(Section.class))
                .append(" where parent_id = ").append(sectionType.getId());
        return load(sql.toString());
    }

    public void remove(Resource resource) throws SQLException, ReflectiveOperationException, ManagerException {
        for (SectionType sectionType : load(resource)) {
            recursiveRemove(sectionType);
        }
    }

    private void recursiveRemove(SectionType sectionType) throws SQLException, ReflectiveOperationException, ManagerException {
        for (SectionType subSectionType : load(sectionType)) {
            recursiveRemove(subSectionType);
        }
        remove(sectionType);
    }

}
