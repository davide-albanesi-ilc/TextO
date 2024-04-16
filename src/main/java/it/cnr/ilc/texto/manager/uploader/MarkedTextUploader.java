package it.cnr.ilc.texto.manager.uploader;

import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Section;
import it.cnr.ilc.texto.domain.SectionType;
import it.cnr.ilc.texto.manager.ResourceManager.Uploader;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author oakgen
 */
public class MarkedTextUploader extends Uploader {

    @Override
    protected String name() {
        return "marked-text";
    }

    @Override
    public String upload(Resource resource, String source, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        source = source.replaceAll("\r", "");
        List<Section> sections = new ArrayList<>();
        Map<String, SectionType> types = new HashMap<>();
        Matcher matcher = Pattern.compile("(#+)\\((.*)\\)\\n").matcher(source);
        Section previous = null;
        Section current;
        int minus = 0;
        int offset;
        int levelCurrent = 0, levelPrevious = 0;
        String info, typeName;
        while (matcher.find()) {
            info = matcher.group(2);
            typeName = getGroupRegex(info, "type=\"(.*?)\"");
            offset = matcher.start() - minus;
            current = new Section();
            current.setResource(resource);
            current.setStart(offset);
            levelCurrent = (int) matcher.group(1).chars().filter(c -> c == '#').count();
            if (levelCurrent == levelPrevious + 1) {
                current.setParent(previous);
                current.setType(getType(types, typeName, resource, previous == null ? null : previous.getType()));
            } else if (levelCurrent == levelPrevious) {
                current.setParent(previous.getParent());
                current.setType(getType(types, typeName, resource, previous.getParent() == null ? null : previous.getParent().getType()));
                previous.setEnd(current.getStart());
            } else if (levelCurrent < levelPrevious) {
                previous.setEnd(current.getStart());
                for (int i = 0; i < levelPrevious - levelCurrent; i++) {
                    previous = previous.getParent();
                    previous.setEnd(current.getStart());
                }
                current.setParent(previous.getParent());
                current.setType(getType(types, typeName, resource, previous.getParent() == null ? null : previous.getParent().getType()));
            } else {
                throw new ManagerException("invalid anchor level " + matcher.group(1));
            }
            current.setTitle(getGroupRegex(info, "title=\"(.*?)\""));
            current.setIndex(getGroupRegex(info, "index=\"(.*?)\""));
            sections.add(current);
            levelPrevious = levelCurrent;
            previous = current;
            minus += matcher.end() - matcher.start();
        }
        while (previous != null) {
            previous.setEnd(source.length() - minus);
            previous = previous.getParent();
        }
        for (Section section : sections) {
            domainManager.create(section);
        }
        source = matcher.replaceAll("");
        if ("false".equalsIgnoreCase(parameters.get("splitline"))) {
            insertSectionRows(sections);
        } else {
            insertSplittedRows(sections, source);
        }
        return source;
    }

    private static String getGroupRegex(String string, String regex) throws ManagerException {
        Matcher matcher = Pattern.compile(regex).matcher(string);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new ManagerException("invalid section attribute " + string);
        }
    }

    private SectionType getType(Map<String, SectionType> types, String name, Resource resource, SectionType parent) throws SQLException, ReflectiveOperationException, ManagerException {
        SectionType type = types.get(name);
        if (type == null) {
            type = new SectionType();
            type.setName(name);
            type.setParent(parent);
            type.setResource(resource);
            domainManager.create(type);
            types.put(name, type);
        }
        return type;
    }

    private void insertSplittedRows(List<Section> sections, String source) throws SQLException, ReflectiveOperationException, ManagerException {
        int s = 0;
        Section section = sections.get(s++);
        while (section.getStart().equals(sections.get(s).getStart())) {
            section = sections.get(s++);
        }
        int start = 0;
        int end = source.indexOf("\n", start, (s < sections.size() ? sections.get(s).getStart() : section.getEnd()));
        int number = 0;
        int relative = 0;
        while (end != -1) {
            Row row = new Row();
            row.setResource(section.getResource());
            row.setSection(section);
            row.setNumber(number++);
            row.setRelative(relative++);
            row.setStart(start);
            row.setEnd(end + 1);
            domainManager.create(row);
            start = end + 1;
            end = source.indexOf("\n", start, (s < sections.size() ? sections.get(s).getStart() : section.getEnd()));
            if (end == -1) {
                if (s < sections.size()) {
                    section = sections.get(s++);
                    while (s < sections.size() && section.getStart().equals(sections.get(s).getStart())) {
                        section = sections.get(s++);
                    }
                    end = source.indexOf("\n", start, (s < sections.size() ? sections.get(s).getStart() : section.getEnd()));
                    relative = 0;
                }
            }
        }
    }

    private void insertSectionRows(List<Section> sections) throws SQLException, ReflectiveOperationException, ManagerException {
        int s = 0;
        int number = 0;
        while (s < sections.size()) {
            Section section = sections.get(s);
            while (s + 1 < sections.size() && section.getStart().equals(sections.get(s + 1).getStart())) {
                section = sections.get(++s);
            }
            Row row = new Row();
            row.setResource(section.getResource());
            row.setSection(section);
            row.setNumber(number++);
            row.setRelative(0);
            row.setStart(section.getStart());
            row.setEnd(s + 1 < sections.size() ? sections.get(s + 1).getStart() : section.getEnd());
            domainManager.create(row);
            s++;
        }
    }

}
