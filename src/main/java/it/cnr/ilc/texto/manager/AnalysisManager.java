package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.domain.Analysis;
import it.cnr.ilc.texto.domain.Annotation;
import it.cnr.ilc.texto.domain.AnnotationFeature;
import it.cnr.ilc.texto.domain.Feature;
import it.cnr.ilc.texto.domain.Layer;
import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.domain.Row;
import it.cnr.ilc.texto.domain.Tagset;
import it.cnr.ilc.texto.domain.TagsetItem;
import it.cnr.ilc.texto.domain.Token;
import it.cnr.ilc.texto.domain.User;
import static it.cnr.ilc.texto.manager.DomainManager.quote;
import it.cnr.ilc.texto.manager.analyzer.OpennlpTokenizerAnalyzer;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.util.DatabaseCreator;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
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
    @Lazy
    @Autowired
    private LayerManager layerManager;
    @Lazy
    @Autowired
    private SectionManager sectionManager;

    private final Map<String, Analyzer> analyzers = new HashMap<>();

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
            tokenLayer = domainManager.loadUnique(Layer.class, "select * from " + quote(Layer.class) + " where name = 'Token'");
            if (tokenLayer != null) {
                sentenceLayer = domainManager.loadUnique(Layer.class, "select * from " + quote(Layer.class) + " where name = 'Sentence'");
                formLayer = domainManager.loadUnique(Layer.class, "select * from " + quote(Layer.class) + " where name = 'Form'");
                lemmaLayer = domainManager.loadUnique(Layer.class, "select * from " + quote(Layer.class) + " where name = 'Lemma'");
                posLayer = domainManager.loadUnique(Layer.class, "select * from " + quote(Layer.class) + " where name = 'POS'");
                featsLayer = domainManager.loadUnique(Layer.class, "select * from " + quote(Layer.class) + " where name = 'Feats'");
                sentenceFetures = domainManager.load(Feature.class, "select * from " + quote(Feature.class) + " where layer_id = " + sentenceLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                tokenFeatures = domainManager.load(Feature.class, "select * from " + quote(Feature.class) + " where layer_id = " + tokenLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                formFeatures = domainManager.load(Feature.class, "select * from " + quote(Feature.class) + " where layer_id = " + formLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                lemmaFeatures = domainManager.load(Feature.class, "select * from " + quote(Feature.class) + " where layer_id = " + lemmaLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                posFeatures = domainManager.load(Feature.class, "select * from " + quote(Feature.class) + " where layer_id = " + posLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
                featsFeatures = domainManager.load(Feature.class, "select * from " + quote(Feature.class) + " where layer_id = " + featsLayer.getId())
                        .stream().collect(Collectors.toMap(e -> e.getName(), e -> e));
            }
        } finally {
            databaseManager.releaseCommitConnection();
        }
    }

    @PostConstruct
    private void initAnalyzers() throws Exception {
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackage(OpennlpTokenizerAnalyzer.class.getPackageName()));
        Collection<Class<? extends Analyzer>> classes = reflections.getSubTypesOf(Analyzer.class);
        for (Class<? extends Analyzer> clazz : classes) {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                Analyzer analyzer = clazz.getConstructor().newInstance();
                analyzer.init(environment, databaseManager, domainManager, monitorManager, this);
                analyzer.init();
                analyzers.put(analyzer.name(), analyzer);
            }
        }
    }

    public Layer getSentenceLayer() {
        return sentenceLayer;
    }

    public Layer getTokenLayer() {
        return tokenLayer;
    }

    public Layer getFormLayer() {
        return formLayer;
    }

    public Layer getLemmaLayer() {
        return lemmaLayer;
    }

    public Layer getPosLayer() {
        return posLayer;
    }

    public Layer getFeatsLayer() {
        return featsLayer;
    }

    public Map<String, Feature> getSentenceFetures() {
        return sentenceFetures;
    }

    public Map<String, Feature> getTokenFeatures() {
        return tokenFeatures;
    }

    public Map<String, Feature> getFormFeatures() {
        return formFeatures;
    }

    public Map<String, Feature> getLemmaFeatures() {
        return lemmaFeatures;
    }

    public Map<String, Feature> getPosFeatures() {
        return posFeatures;
    }

    public Map<String, Feature> getFeatsFeatures() {
        return featsFeatures;
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
        layerManager.importLayers(lines);
        loadLayers();
    }

    public List<Map<String, Object>> getUpos() throws SQLException {
        String sql = "select i.name, i.description from " + quote(Tagset.class) + " t join " + quote(TagsetItem.class) + " i on t.id = i.tagset_id where t.name = 'UPOS'";
        return databaseManager.query(sql);
    }

    public Set<String> getAnalyzers() {
        return analyzers.keySet();
    }

    public void analyze(Resource resource, User user, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        if (tokenLayer == null) {
            throw new ManagerException("analysis layers have not been initialized");
        }
        if (sectionManager.load(resource).isEmpty()) {
            throw new ManagerException("resource have not a valid section");
        }
        String name = parameters.getOrDefault("analyzer", environment.getProperty("analysis.default-analyzer", "opennlp-tokenizer"));
        Analyzer analyzer = analyzers.get(name);
        if (analyzer == null) {
            throw new ManagerException("invalid analyzer " + name);
        }
        if (Boolean.parseBoolean(parameters.getOrDefault("replace", "false"))) {
            removeAnalysis(resource);
        } else if (isAnalyze(resource)) {
            throw new ManagerException("already analyzed");
        }
        analyzer.analyze(resource, user, parameters);
    }

    public boolean isAnalyze(Resource resource) throws SQLException {
        String sql = "select count(*) from Token where resource_id = " + resource.getId();
        return databaseManager.queryFirst(sql, Number.class).intValue() > 0;
    }

    public void removeAnalysis(Resource resource) throws SQLException, ReflectiveOperationException, ManagerException {
        String layerIds = sentenceLayer.getId() + ", " + tokenLayer.getId() + ", " + formLayer.getId() + ", " + lemmaLayer.getId() + ", " + posLayer.getId() + ", " + featsLayer.getId();
        databaseManager.update("delete from " + quote(Analysis.class) + " where resource_id = " + resource.getId());
        databaseManager.update("delete from " + quote(Token.class) + " where resource_id = " + resource.getId());
        databaseManager.update("delete from " + quote(AnnotationFeature.class) + " where annotation_id in (select id from " + quote(Annotation.class) + " where layer_id in (" + layerIds + ") and resource_id = " + resource.getId() + ")");
        databaseManager.update("delete from " + quote(Annotation.class) + " where layer_id in (" + layerIds + ") and resource_id = " + resource.getId());
    }

    public static abstract class Analyzer {

        protected Environment environment;
        protected DatabaseManager databaseManager;
        protected DomainManager domainManager;
        protected MonitorManager monitorManager;
        protected AnalysisManager analysisManager;

        private void init(Environment environment, DatabaseManager databaseManager, DomainManager domainManager, MonitorManager monitorManager, AnalysisManager analysisManager) {
            this.environment = environment;
            this.databaseManager = databaseManager;
            this.domainManager = domainManager;
            this.monitorManager = monitorManager;
            this.analysisManager = analysisManager;
        }

        protected void init() throws SQLException, ReflectiveOperationException, ManagerException {
        }

        protected abstract String name();

        protected abstract void analyze(Resource resource, User user, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException;

        public class RowIndexes {

            private final Long[] ids;
            private final int[] starts;

            public RowIndexes(Resource resource) throws SQLException {
                String sql = "select start, id from " + quote(Row.class) + " where resource_id = " + resource.getId() + " order by start";
                List<Map<String, Object>> result = databaseManager.query(sql);
                starts = new int[result.size()];
                ids = new Long[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    starts[i] = ((Number) result.get(i).get("start")).intValue();
                    ids[i] = ((Number) result.get(i).get("id")).longValue();
                }
            }

            public Long getId(int start) {
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
}
