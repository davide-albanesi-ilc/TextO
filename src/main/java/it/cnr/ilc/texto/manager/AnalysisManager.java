package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Analysis;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Entity;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.FeatureType;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.domain.Token;
import it.cnr.ilc.texto.domain.User;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.util.DatabaseCreator;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class AnalysisManager extends EntityManager<Analysis> {

    @Autowired
    private MonitorManager monitorManager;
    @Lazy
    @Autowired
    private ResourceManager resourceManager;

    private Layer sentenceLayer;
    private Layer tokenLayer;
    private Layer formLayer;
    private Layer lemmaLayer;
    private Layer posLayer;
    private Layer featsLayer;
    private Map<String, Feature> sentenceFetures;
    private Map<String, Feature> tokenFeatures;
    private Map<String, Feature> formFeatures;
    private Map<String, Feature> lemmaFeatures;
    private Map<String, Feature> posFeatures;
    private Map<String, Feature> featsFeatures;

    @PostConstruct
    private void loadLayers() throws SQLException, ReflectiveOperationException {
        try {
            tokenLayer = domainManager.loadUnique(Layer.class, "select * from Layer where name = 'Token'");
            if (tokenLayer != null) {
                sentenceLayer = domainManager.loadUnique(Layer.class, "select * from Layer where name = 'Sentence'");
                formLayer = domainManager.loadUnique(Layer.class, "select * from Layer where name = 'Form'");
                lemmaLayer = domainManager.loadUnique(Layer.class, "select * from Layer where name = 'Lemma'");
                posLayer = domainManager.loadUnique(Layer.class, "select * from Layer where name = 'POS'");
                featsLayer = domainManager.loadUnique(Layer.class, "select * from Layer where name = 'Feats'");
                sentenceFetures = domainManager.load(Feature.class, "select * from Feature where layer_id = " + sentenceLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                tokenFeatures = domainManager.load(Feature.class, "select * from Feature where layer_id = " + tokenLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                formFeatures = domainManager.load(Feature.class, "select * from Feature where layer_id = " + formLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                lemmaFeatures = domainManager.load(Feature.class, "select * from Feature where layer_id = " + lemmaLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                posFeatures = domainManager.load(Feature.class, "select * from Feature where layer_id = " + posLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                featsFeatures = domainManager.load(Feature.class, "select * from Feature where layer_id = " + featsLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
            }
        } finally {
            databaseManager.releaseCommitConnection();
        }
    }

    @Override
    protected Class<Analysis> entityClass() {
        return Analysis.class;
    }

    @Override
    public String getLog(Analysis analysis) throws SQLException, ReflectiveOperationException, ManagerException {
        if (analysis.getResource() == null || analysis.getValue() == null) {
            return "" + analysis.getId();
        } else {
            return resourceManager.getLog(resourceManager.load(analysis.getResource().getId())) + " " + analysis.getValue();
        }
    }

    public void initLayers() throws SQLException, ReflectiveOperationException, ManagerException {
        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(DatabaseCreator.class.getResource("/analysis.init").toURI()));
        } catch (URISyntaxException | IOException ex) {
            throw new RuntimeException(ex);
        }
        List<Entity> entities = new ArrayList<>();
        Layer layer = null;
        Tagset tagset = null;
        TagsetItem tagsetItem;
        Feature feature;
        Map<String, Tagset> tagsets = new HashMap<>();
        String[] split;
        for (String line : lines) {
            split = line.split("\t");
            if (split[0].equals(Layer.class.getSimpleName())) {
                layer = new Layer();
                layer.setName(split[1]);
                layer.setDescription(split[2]);
                layer.setColor(split[3]);
                entities.add(layer);
            } else if (split[0].equals(Tagset.class.getSimpleName())) {
                tagset = new Tagset();
                tagset.setName(split[1]);
                tagset.setDescription(split[2]);
                entities.add(tagset);
                tagsets.put(tagset.getName(), tagset);
            } else if (split[0].equals(TagsetItem.class.getSimpleName())) {
                tagsetItem = new TagsetItem();
                tagsetItem.setTagset(tagset);
                tagsetItem.setName(split[1]);
                tagsetItem.setDescription(split[2]);
                entities.add(tagsetItem);
            } else if (split[0].equals(Feature.class.getSimpleName())) {
                feature = new Feature();
                feature.setLayer(layer);
                feature.setName(split[1]);
                feature.setDescription(split[2]);
                feature.setType(FeatureType.valueOf(split[3]));
                if (feature.getType().equals(FeatureType.TAGSET)) {
                    feature.setTagset(tagsets.get(split[4]));
                }
                entities.add(feature);
            }
        }
        domainManager.create(entities);
        loadLayers();
    }

    public void analize(Resource resource, User user, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        if (tokenLayer == null) {
            throw new ManagerException("analysis layers have not been initialized");
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get("/Users/oakgen/Desktop/conllu.txt"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        monitorManager.setMax(lines.size());
        RowIndexes rowIndexes = new RowIndexes(resource);
        List<Entity> entities = new ArrayList<>();
        String[] split, subsplit, offset, feats;
        String value = null, form = null, lemma = null, pos = null;
        Annotation annotation;
        AnnotationFeature annotationFeature;
        Feature feature;
        Token token = null;
        Analysis analysis;
        StringBuilder sql;
        int start = 0, end = 0, sentenceStart = -1, sentenceId = 0, number = -1;
        for (String line : lines) {
            // SENTENCE
            if (line.startsWith("# text") && sentenceStart != -1) {
                annotation = new Annotation();
                annotation.setResource(resource);
                annotation.setLayer(sentenceLayer);
                annotation.setStart(sentenceStart);
                annotation.setEnd(end);
                annotation.setUser(user);
                entities.add(annotation);
                annotationFeature = new AnnotationFeature();
                annotationFeature.setAnnotation(annotation);
                annotationFeature.setFeature(sentenceFetures.get("Id"));
                annotationFeature.setValue("" + sentenceId++);
                entities.add(annotationFeature);
                sentenceStart = -1;
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                if (sentenceStart == -1) {
                    sentenceStart = start;
                }
                split = line.split("\\t");
                // TOKEN
                if (!split[9].equals("_")) {
                    offset = split[9].split("\\|");
                    start = Integer.parseInt(offset[0].substring(11));
                    end = Integer.parseInt(offset[1].substring(9));
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(tokenLayer);
                    annotation.setStart(start);
                    annotation.setEnd(end);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(tokenFeatures.get("Id"));
                    annotationFeature.setValue(split[0]);
                    entities.add(annotationFeature);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(tokenFeatures.get("Token"));
                    annotationFeature.setValue(split[1]);
                    entities.add(annotationFeature);
                    value = annotationFeature.getValue();
                }
                // FORM
                if (!split[0].contains("-")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(formLayer);
                    annotation.setStart(start);
                    annotation.setEnd(end);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(formFeatures.get("Id"));
                    annotationFeature.setValue(split[0]);
                    entities.add(annotationFeature);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(formFeatures.get("Form"));
                    annotationFeature.setValue(split[1]);
                    entities.add(annotationFeature);
                    form = annotationFeature.getValue();
                }
                // LEMMA
                if (!split[2].equals("_")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(lemmaLayer);
                    annotation.setStart(start);
                    annotation.setEnd(end);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(lemmaFeatures.get("Lemma"));
                    annotationFeature.setValue(split[2]);
                    entities.add(annotationFeature);
                    lemma = annotationFeature.getValue();
                }
                // POS
                if (!split[3].equals("_")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(posLayer);
                    annotation.setStart(start);
                    annotation.setEnd(end);
                    annotation.setUser(user);
                    entities.add(annotation);
                    annotationFeature = new AnnotationFeature();
                    annotationFeature.setAnnotation(annotation);
                    annotationFeature.setFeature(posFeatures.get("UPOS"));
                    annotationFeature.setValue(split[3]);
                    entities.add(annotationFeature);
                    pos = annotationFeature.getValue();
                }
                // FEATS
                if (!split[5].equals("_")) {
                    annotation = new Annotation();
                    annotation.setResource(resource);
                    annotation.setLayer(featsLayer);
                    annotation.setStart(start);
                    annotation.setEnd(end);
                    annotation.setUser(user);
                    entities.add(annotation);
                    feats = split[5].split("\\|");
                    for (String feat : feats) {
                        subsplit = feat.split("\\=");
                        annotationFeature = new AnnotationFeature();
                        annotationFeature.setAnnotation(annotation);
                        feature = featsFeatures.get(subsplit[0]);
                        if (feature == null) {
                            throw new ManagerException("morphological feature not found: " + subsplit[0]);
                        }
                        annotationFeature.setFeature(feature);
                        annotationFeature.setValue(subsplit[1]);
                        entities.add(annotationFeature);
                    }
                }
                if (!split[9].equals("_")) {
                    number++;
                    token = new Token();
                    token.setResource(resource);
                    token.setRow(Entity.newGhost(Row.class, rowIndexes.getId(start)));
                    token.setNumber(number);
                    token.setStart(start);
                    token.setEnd(end);
                    entities.add(token);
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
                }
            }
            monitorManager.next();
        }
        domainManager.create(entities);
    }

    private class RowIndexes {

        private final Long[] ids;
        private final int[] starts;

        private RowIndexes(Resource resource) throws SQLException {
            String sql = "select start, id from `Row` where resource_id = " + resource.getId() + " order by start";
            List<Map<String, Object>> result = databaseManager.query(sql);
            starts = new int[result.size()];
            ids = new Long[result.size()];
            for (int i = 0; i < result.size(); i++) {
                starts[i] = ((Number) result.get(i).get("start")).intValue();
                ids[i] = ((Number) result.get(i).get("id")).longValue();
            }
        }

        private Long getId(int start) {
            int i = 0, N = starts.length - 1;
            while (i != N) {
                if (starts[i] <= start && start < starts[i + 1]) {
                    N = i;
                } else {
                    i++;
                }
            }
            return ids[i];
        }
    }

}
