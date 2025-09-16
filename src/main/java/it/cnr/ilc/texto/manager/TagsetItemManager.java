package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.exception.ManagerException;
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
    @Lazy
    @Autowired
    private AnalysisManager analysisManager;

    @Trigger(event = Trigger.Event.PRE_CREATE)
    @Trigger(event = Trigger.Event.PRE_UPDATE)
    @Trigger(event = Trigger.Event.PRE_REMOVE)
    public void checkAnalysis(TagsetItem previous, TagsetItem tagsetItem) throws SQLException, ReflectiveOperationException, ManagerException {
        if (analysisManager.isAnalysisTagset(tagsetItem.getTagset())) {
            throw new ManagerException("analysis is locked");
        }
    }

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
