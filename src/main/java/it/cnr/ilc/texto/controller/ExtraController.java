package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.domain.Action;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.DomainManager;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("extra")
public class ExtraController extends Controller {

    @Autowired
    private DatabaseManager databaseManager;

    @Autowired
    private DomainManager domainManager;

    @GetMapping("token_annotation")
    public String tokenAnnotation(@RequestParam Map<String, String> params) throws ForbiddenException, ManagerException, SQLException {
        accessManager.checkAccess(System.class, Action.WRITE);
        List<Map<String, Object>> tokens = databaseManager.query("select id, start, end from Token order by start, end");
        List<Map<String, Object>> annotations = databaseManager.query("select id, start, end from Annotation order by start, end");
        int annotationIdx = 0, checkIdx;
        int tokenStart, tokenEnd, annotationStart, annotationEnd;
        monitorManager.setMax(tokens.size());
        for (Map<String, Object> token : tokens) {
            tokenStart = start(token);
            tokenEnd = end(token);
            while (annotationIdx < annotations.size() && end(annotations.get(annotationIdx)) < tokenStart) {
                annotationIdx++;
            }
            checkIdx = annotationIdx;
            while (checkIdx < annotations.size()) {
                Map<String, Object> annotation = annotations.get(checkIdx);
                annotationStart = start(annotation);
                annotationEnd = end(annotation);
                if (annotationStart > tokenStart) {
                    break;
                }
                if (annotationStart <= tokenStart && tokenEnd <= annotationEnd) {
                    databaseManager.update("insert into _tokenannotation values(" + id(token) + ", " + id(annotation) + ")");
                }
                checkIdx++;
            }
            monitorManager.next();
        }
        return "OK";
    }

    private Long id(Map<String, Object> map) {
        return ((Number) map.get("id")).longValue();
    }

    private int start(Map<String, Object> map) {
        return ((Number) map.get("start")).intValue();
    }

    private int end(Map<String, Object> map) {
        return ((Number) map.get("end")).intValue();
    }

    @GetMapping("syntagm_semantics/export")
    public String syntagmSemanticsExport(@RequestParam Map<String, String> params) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        accessManager.checkAccess(System.class, Action.WRITE);
        int gstart, gend, mstart, mend;
        String index;
        String text = databaseManager.queryFirst("select text from _text where resource_id = 123", String.class);
        String sql = """
                     select distinct s.index, g.start gstart, g.end gend, m.start mstart, m.end mend
                     from Annotation g 
                     left join Annotation m on m.resource_id = g.resource_id and m.layer_id = 20077428 and (m.start = g.start or m.end = g.end)
                     join _tokenannotation _t on _t.annotation_id = g.id
                     join Token t on t.id = _t.token_id
                     join `Row` r on r.id = t.row_id
                     join Section s on s.id = r.section_id
                     where g.resource_id = 123 and g.layer_id = 20721883
                     order by g.start
                     """;
        List<Map<String, Object>> list = databaseManager.query(sql);
        for (Map<String, Object> map : list) {
            index = (String) map.get("index");
            gstart = ((Number) map.get("gstart")).intValue();
            gend = ((Number) map.get("gend")).intValue();
            if (map.get("mstart") != null) {
                mstart = ((Number) map.get("mstart")).intValue();
                mend = ((Number) map.get("mend")).intValue();
                System.out.println(index + "\t" + text.substring(gstart, gend) + "\t" + text.substring(mstart, mend));
            } else {
                System.out.println(index + "\t" + text.substring(gstart, gend) + "\t");
            }
        }
        return "OK";
    }

    @GetMapping("syntagm_semantics/monorematica")
    public String syntagmSemantics(@RequestParam Map<String, String> params) throws ForbiddenException, SQLException, ReflectiveOperationException, ManagerException {
        accessManager.checkAccess(System.class, Action.WRITE);
        Feature typeFeature = domainManager.load(Feature.class, 25364634l);
        String monorematica = "Parola monorematica";
        String sql = """
                     select a.*
                     from Annotation a
                     where layer_id = 20077428 
                      and (select count(token_id) from _tokenannotation where annotation_id = a.id) = 1
                     order by a.start
                     """;
        List<Annotation> annotations = domainManager.load(Annotation.class, sql);
        List<AnnotationFeature> annotationFeatures;
        AnnotationFeature typeAnnotationFeature;
        monitorManager.setMax(annotations.size());
        for (Annotation annotation : annotations) {
            annotationFeatures = domainManager.load(AnnotationFeature.class, "select * from AnnotationFeature where annotation_id = " + annotation.getId());
            typeAnnotationFeature = null;
            for (AnnotationFeature annotationFeature : annotationFeatures) {
                if (annotationFeature.getFeature().equals(typeFeature)) {
                    typeAnnotationFeature = annotationFeature;
                    break;
                }
            }
            if (typeAnnotationFeature == null) {
                typeAnnotationFeature = new AnnotationFeature();
                typeAnnotationFeature.setAnnotation(annotation);
                typeAnnotationFeature.setFeature(typeFeature);
                typeAnnotationFeature.setValue(monorematica);
                domainManager.create(typeAnnotationFeature);
            } else if (!typeAnnotationFeature.getValue().equals(monorematica)) {
                typeAnnotationFeature.setValue(monorematica);
                databaseManager.update("update AnnotationFeature set value = '" + monorematica + "' where id = " + typeAnnotationFeature.getId());
            }
            monitorManager.next();
        }
        return "OK";
    }

}
