package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class LayerGroup extends Entity {

    private Layer layer;
    private Group group;
    private Action action;

    @Required
    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    @Required
    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Required
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
