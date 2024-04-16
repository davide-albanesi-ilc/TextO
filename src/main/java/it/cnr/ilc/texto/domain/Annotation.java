package it.cnr.ilc.texto.domain;

import it.cnr.ilc.texto.domain.annotation.Indexed;
import it.cnr.ilc.texto.domain.annotation.Required;

/**
 *
 * @author oakgen
 */
public class Annotation extends Entity implements Userable {

    private Layer layer;
    private Resource resource;
    private Integer start;
    private Integer end;
    private User user;

    @Required
    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    @Required
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Required
    @Indexed
    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    @Required
    public Integer getEnd() {
        return end;
    }

    @Indexed
    public void setEnd(Integer end) {
        this.end = end;
    }

    @Override
    @Required
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

}
