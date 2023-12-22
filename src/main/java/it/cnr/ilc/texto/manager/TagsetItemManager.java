package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.TagsetItem;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class TagsetItemManager extends EntityManager<TagsetItem> {

    @Override
    protected Class<TagsetItem> entityClass() {
        return TagsetItem.class;
    }

    @Override
    public String getLog(TagsetItem tagsetItem) {
        return tagsetItem.getName() != null ? tagsetItem.getName() : "" + tagsetItem.getId();
    }

}
