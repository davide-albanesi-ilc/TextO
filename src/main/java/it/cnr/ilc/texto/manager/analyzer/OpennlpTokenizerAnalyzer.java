package it.cnr.ilc.texto.manager.analyzer;

import it.cnr.ilc.texto.domain.Analysis;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Token;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.AnalysisManager.Analyzer;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

/**
 *
 * @author oakgen
 */
public class OpennlpTokenizerAnalyzer extends Analyzer {

    @Override
    protected String name() {
        return "opennlp-tokenizer";
    }

    private static Tokenizer getTokenizer(String lang) throws ManagerException {
        try (InputStream input = OpennlpTokenizerAnalyzer.class.getResourceAsStream("/opennlp-models/" + lang + "-token.bin")) {
            TokenizerModel model = new TokenizerModel(input);
            return new TokenizerME(model);
        } catch (IOException e) {
            throw new ManagerException("opennlp lang model not found: " + lang);
        }
    }

    @Override
    protected void analyze(Resource resource, User user, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        String lang = parameters.get("lang");
        if (lang == null) {
            lang = environment.getProperty("analysis.default-lang", "it");
        }
        Tokenizer tokenizer = getTokenizer(lang);
        String sql = "select text from _text where resource_id = " + resource.getId();
        String text = databaseManager.queryFirst(sql, String.class);
        RowIndexes rowIndexes = new RowIndexes(resource);
        List<Entity> entities = new ArrayList<>();
        int number = 0;
        Token token;
        Analysis analysis;
        Annotation annotation;
        AnnotationFeature annotationFeature;
        Span[] spans = tokenizer.tokenizePos(text);
        String[] tokens = tokenizer.tokenize(text);
        monitorManager.setMax(spans.length);
        for (int i = 0; i < tokens.length; i++) {

            token = new Token();
            token.setResource(resource);
            token.setRow(Entity.newGhost(Row.class, rowIndexes.getId(spans[i].getStart())));
            token.setNumber(number);
            token.setStart(spans[i].getStart());
            token.setEnd(spans[i].getEnd());
            entities.add(token);

            analysis = new Analysis();
            analysis.setResource(resource);
            analysis.setToken(token);
            analysis.setValue(tokens[i]);
            analysis.setForm("");
            analysis.setLemma("");
            analysis.setPos("");
            entities.add(analysis);

            annotation = new Annotation();
            annotation.setResource(resource);
            annotation.setLayer(analysisManager.getTokenLayer());
            annotation.setStart(spans[i].getStart());
            annotation.setEnd(spans[i].getEnd());
            annotation.setUser(user);
            entities.add(annotation);

            annotationFeature = new AnnotationFeature();
            annotationFeature.setAnnotation(annotation);
            annotationFeature.setFeature(analysisManager.getTokenFeatures().get("Id"));
            annotationFeature.setValue("" + number);
            entities.add(annotationFeature);
            annotationFeature = new AnnotationFeature();
            annotationFeature.setAnnotation(annotation);
            annotationFeature.setFeature(analysisManager.getTokenFeatures().get("Token"));
            annotationFeature.setValue(tokens[i]);
            entities.add(annotationFeature);

            number++;
            monitorManager.next();
        }
        domainManager.create(entities);
    }

}
