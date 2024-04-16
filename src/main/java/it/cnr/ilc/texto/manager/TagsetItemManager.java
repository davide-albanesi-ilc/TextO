package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.TagsetItem;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class TagsetItemManager extends EntityManager<TagsetItem> {

    @Lazy
    @Autowired
    private TagsetManager tagsetManager;

    @Override
    protected Class<TagsetItem> entityClass() {
        return TagsetItem.class;
    }

    @Override
    public String getLog(TagsetItem tagsetItem) throws SQLException, ReflectiveOperationException {
        if (tagsetItem.getTagset() == null || tagsetItem.getName() == null) {
            return "" + tagsetItem.getId();
        } else {
            return tagsetManager.getLog(tagsetManager.load(tagsetItem.getTagset().getId())) + " "
                    + tagsetItem.getName();
        }
    }

}
