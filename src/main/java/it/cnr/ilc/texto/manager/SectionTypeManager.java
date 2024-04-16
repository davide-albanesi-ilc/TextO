package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.SectionType;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class SectionTypeManager extends EntityManager<SectionType> {

    @Override
    protected Class<SectionType> entityClass() {
        return SectionType.class;
    }

    @Override
    public String getLog(SectionType sectionType) {
        return sectionType.getName() != null ? sectionType.getName() : "" + sectionType.getId();
    }

}
