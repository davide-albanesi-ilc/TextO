package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.FeatureType;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.manager.annotation.Trigger;
import it.cnr.ilc.texto.manager.annotation.Trigger.Event;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class LayerManager extends EntityManager<Layer> {

    @Lazy
    @Autowired
    private FeatureManager featureManager;

    @Override
    protected Class<Layer> entityClass() {
        return Layer.class;
    }

    @Override
    public String getLog(Layer layer) {
        return layer.getName() != null ? layer.getName() : "" + layer.getId();
    }

    @Trigger(event = Event.PRE_REMOVE)
    public void removeFeatures(Layer previous, Layer layer) throws SQLException, ReflectiveOperationException, ManagerException {
        featureManager.remove(layer);
    }

    public void importLayers(List<String> lines) throws SQLException, ReflectiveOperationException, ManagerException {
        List<Entity> entities = new ArrayList<>();
        Layer layer = null;
        Tagset tagset = null;
        TagsetItem tagsetItem;
        Feature feature;
        Map<String, Tagset> tagsets = new HashMap<>();
        String[] split;
        for (String line : lines) {
            split = line.split("\t");
            if (split[0].equals(Layer.class.getSimpleName())) {
                layer = new Layer();
                layer.setName(split[1]);
                layer.setDescription(split[2]);
                layer.setColor(split[3]);
                entities.add(layer);
            } else if (split[0].equals(Tagset.class.getSimpleName())) {
                tagset = new Tagset();
                tagset.setName(split[1]);
                tagset.setDescription(split[2]);
                entities.add(tagset);
                tagsets.put(tagset.getName(), tagset);
            } else if (split[0].equals(TagsetItem.class.getSimpleName())) {
                tagsetItem = new TagsetItem();
                tagsetItem.setTagset(tagset);
                tagsetItem.setName(split[1]);
                tagsetItem.setDescription(split[2]);
                entities.add(tagsetItem);
            } else if (split[0].equals(Feature.class.getSimpleName())) {
                feature = new Feature();
                feature.setLayer(layer);
                feature.setName(split[1]);
                feature.setDescription(split[2]);
                feature.setType(FeatureType.valueOf(split[3]));
                if (feature.getType().equals(FeatureType.TAGSET)) {
                    feature.setTagset(tagsets.get(split[4]));
                }
                entities.add(feature);
            }
        }
        domainManager.create(entities);
    }
}
