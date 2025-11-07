package it.cnr.ilc.texto.manager.analyzer;

import it.cnr.ilc.texto.domain.Analysis;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Token;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.AnalysisManager.Analyzer;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author oakgen
 */
public abstract class ConlluAnalyzer extends Analyzer {

    public abstract List<String> getConllu(Resource resource, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException;

    @Override
    protected void analyze(Resource resource, User user, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        List<String> lines = getConllu(resource, parameters);
        monitorManager.setMax(lines.size());
        RowIndexes rowIndexes = new RowIndexes(resource);
        List<Entity> entities = new ArrayList<>();
        String[] split, subsplit, feats;
        String value = null, form = null, lemma = null, pos = null;
        int sentenceStart = -1, sentenceId = 0, number = -1, multi = 0;
        int[] offsets = new int[]{0, 0};
        Annotation annotation;
        AnnotationFeature annotationFeature;
        Feature feature;
        Token token = null;
        Analysis analysis;
        for (String line : lines) {
            // SENTENCE
            if (line.startsWith("# text") && sentenceStart != -1) {
                annotation = new Annotation();
                annotation.setResource(resource);
                annotation.setLayer(analysisManager.getSentenceLayer());
                annotation.setStart(sentenceStart);
                annotation.setEnd(offsets[1]);
                annotation.setUser(user);
                entities.add(annotation);
                annotationFeature = new AnnotationFeature();
                annotationFeature.setAnnotation(annotation);
                annotationFeature.setFeature(analysisManager.getSentenceFetures().get("Id"));
                annotationFeature.setValue("" + sentenceId++);
                entities.add(annotationFeature);
                sentenceStart = -1;
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                split = line.split("\\t");
                // TOKEN
                if (multi == 0) {
                    offsets = getOffsets(split[9]);
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(analysisManager.getTokenLayer());
                    annotation.setStart(offsets[0]);
                    annotation.setEnd(offsets[1]);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(analysisManager.getTokenFeatures().get("Id"));
                    annotationFeature.setValue(split[0]);
                    entities.add(annotationFeature);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(analysisManager.getTokenFeatures().get("Token"));
                    annotationFeature.setValue(split[1]);
                    entities.add(annotationFeature);
                    value = annotationFeature.getValue();
                }
                // FORM
                if (!split[0].contains("-")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(analysisManager.getFormLayer());
                    annotation.setStart(offsets[0]);
                    annotation.setEnd(offsets[1]);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(analysisManager.getFormFeatures().get("Id"));
                    annotationFeature.setValue(split[0]);
                    entities.add(annotationFeature);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(analysisManager.getFormFeatures().get("Form"));
                    annotationFeature.setValue(split[1]);
                    entities.add(annotationFeature);
                    form = annotationFeature.getValue();
                }
                // LEMMA
                if (!split[2].equals("_")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(analysisManager.getLemmaLayer());
                    annotation.setStart(offsets[0]);
                    annotation.setEnd(offsets[1]);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(analysisManager.getLemmaFeatures().get("Lemma"));
                    annotationFeature.setValue(split[2]);
                    entities.add(annotationFeature);
                    lemma = annotationFeature.getValue();
                }
                // POS
                if (!split[3].equals("_")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(analysisManager.getPosLayer());
                    annotation.setStart(offsets[0]);
                    annotation.setEnd(offsets[1]);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(analysisManager.getPosFeatures().get("UPOS"));
                    annotationFeature.setValue(split[3]);
                    entities.add(annotationFeature);
                    pos = annotationFeature.getValue();
                }
                // FEATS
                if (!split[5].equals("_")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(analysisManager.getFeatsLayer());
                    annotation.setStart(offsets[0]);
                    annotation.setEnd(offsets[1]);
                    annotation.setUser(user);
                    entities.add(annotation);
                    feats = split[5].split("\\|");
                    for (String feat : feats) {
                        subsplit = feat.split("\\=");
                        annotationFeature = new AnnotationFeature();
                        annotationFeature.setAnnotation(annotation);
                        feature = analysisManager.getFeatsFeatures().get(subsplit[0]);
                        if (feature == null) {
                            throw new ManagerException("morphological feature not found: " + subsplit[0]);
                        }
                        annotationFeature.setFeature(feature);
                        annotationFeature.setValue(subsplit[1]);
                        entities.add(annotationFeature);
                    }
                }
                if (multi == 0) {
                    number++;
                    token = new Token();
                    token.setResource(resource);
                    token.setRow(Entity.newGhost(Row.class, rowIndexes.getId(offsets[0])));
                    token.setNumber(number);
                    token.setStart(offsets[0]);
                    token.setEnd(offsets[1]);
                    entities.add(token);
                } else {
                    multi--;
                }
                if (!split[0].contains("-")) {
                    analysis = new Analysis();
                    analysis.setResource(resource);
                    analysis.setToken(token);
                    analysis.setValue(value);
                    analysis.setForm(form);
                    analysis.setLemma(lemma);
                    analysis.setPos(pos);
                    entities.add(analysis);
                } else {
                    split = split[0].split("-");
                    multi = Integer.parseInt(split[1]) - Integer.parseInt(split[0]) + 1;
                }
                if (sentenceStart == -1) {
                    sentenceStart = offsets[0];
                }
            }
            monitorManager.next();
        }
        domainManager.create(entities);
    }

    private int[] getOffsets(String input) throws ManagerException {
        int[] offsets = new int[]{-1, -1};
        String[] splits = input.split("\\|");
        for (String split : splits) {
            if (split.startsWith("start_char")) {
                offsets[0] = Integer.parseInt(split.substring(11));
            } else if (split.startsWith("end_char")) {
                offsets[1] = Integer.parseInt(split.substring(9));
            }
        }
        if (offsets[0] == -1 || offsets[1] == -1) {
            throw new ManagerException("start_char or end_char not found: " + input);
        }
        return offsets;
    }
}
