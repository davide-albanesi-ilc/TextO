package it.cnr.ilc.texto.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.cnr.ilc.texto.domain.annotation.Required;
import it.cnr.ilc.texto.domain.annotation.Unique;

/**
 *
 * @author oakgen
 */
public class Feature extends Entity {

    private String name;
    private String description;
    private Layer layer;
    private FeatureType type;
    private Tagset tagset;

    @Required
    @Unique(group = "layer")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Required
    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    @Required
    public FeatureType getType() {
        return type;
    }

    public void setType(FeatureType type) {
        this.type = type;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Tagset getTagset() {
        return tagset;
    }

    public void setTagset(Tagset tagset) {
        this.tagset = tagset;
    }

}
