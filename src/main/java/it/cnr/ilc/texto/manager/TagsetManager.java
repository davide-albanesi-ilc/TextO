package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class TagsetManager extends EntityManager<Tagset> {

    @Autowired
    private TagsetItemManager tagsetItemManager;

    @Override
    protected Class<Tagset> entityClass() {
        return Tagset.class;
    }

    @Override
    public String getLog(Tagset tagset) throws SQLException, ReflectiveOperationException, ManagerException {
        return tagset.getName() != null ? tagset.getName() : "" + tagset.getId();
    }

    public List<TagsetItem> getItems(Tagset tagset) throws SQLException, ReflectiveOperationException, ManagerException {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from ").append(DomainManager.quote(TagsetItem.class))
                .append("where status = 1 and tagset_id = ").append(tagset.getId());
        return tagsetItemManager.load(sql.toString());
    }

}
