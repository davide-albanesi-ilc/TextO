package it.cnr.ilc.texto.manager.uploader;

import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.domain.SectionType;
import it.cnr.ilc.texto.manager.ResourceManager.Uploader;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author oakgen
 */
public class PlainTextUploader extends Uploader {

    @Override
    protected String name() {
        return "plain-text";
    }

    @Override
    protected String upload(Resource resource, String source, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        List<Entity> entities = new ArrayList<>();
        Section section = null;
        if ("true".equalsIgnoreCase(parameters.get("section"))) {
            SectionType sectionType = new SectionType();
            sectionType.setResource(resource);
            sectionType.setName(resource.getName());
            entities.add(sectionType);
            section = new Section();
            section.setResource(resource);
            section.setType(sectionType);
            section.setTitle(resource.getName());
            section.setIndex("-");
            section.setStart(0);
            section.setEnd(source.length());
            entities.add(section);
        }
        source = source.replaceAll("\r", "");
        int start = 0;
        int end = source.indexOf("\n");
        int number = 0;
        while (end != -1) {
            Row row = new Row();
            row.setResource(resource);
            row.setSection(section);
            row.setNumber(number);
            row.setRelative(section == null ? null : 0);
            row.setStart(start);
            row.setEnd(end + 1);
            entities.add(row);
            start = end + 1;
            end = source.indexOf("\n", start);
            number++;
        }
        if (start != source.length()) {
            Row row = new Row();
            row.setResource(resource);
            row.setSection(section);
            row.setNumber(number);
            row.setRelative(section == null ? null : 0);
            row.setStart(start);
            row.setEnd(source.length());
            entities.add(row);
        }
        domainManager.create(entities);
        return source;
    }

}
