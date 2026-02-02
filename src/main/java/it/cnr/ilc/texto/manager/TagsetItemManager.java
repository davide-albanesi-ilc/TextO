package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.Objects;
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
    @Lazy
    @Autowired
    private AnnotationFeatureManager annotationFeatureManager;

    @Trigger(event = Trigger.Event.PRE_CREATE)
    @Trigger(event = Trigger.Event.PRE_UPDATE)
    @Trigger(event = Trigger.Event.PRE_REMOVE)
    public void checkAnalysis(TagsetItem previous, TagsetItem tagsetItem) throws SQLException, ReflectiveOperationException, ManagerException {
        if (analysisManager.isAnalysisTagset(tagsetItem.getTagset())) {
            throw new ManagerException("analysis is locked");
        }
    }

    @Trigger(event = Trigger.Event.POST_UPDATE)
    public void checkUpdate(TagsetItem previous, TagsetItem tagsetItem) throws SQLException, ReflectiveOperationException, ManagerException {
        if (!Objects.equals(previous.getName(), tagsetItem.getName())) {
            annotationFeatureManager.tagsetValueMultipleUpdate(tagsetItem.getTagset(), previous.getName(), tagsetItem.getName());
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
