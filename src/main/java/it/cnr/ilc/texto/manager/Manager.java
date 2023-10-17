package it.cnr.ilc.texto.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 *
 * @author oakgen
 */
public abstract class Manager {

    @Autowired
    protected Environment environment;

}
