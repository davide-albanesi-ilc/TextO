package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class LayerUser extends Entity {

    private Layer layer;
    private User user;
    private Action action;

    @Required
    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    @Required
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Required
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
