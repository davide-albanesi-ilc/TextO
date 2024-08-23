package it.cnr.ilc.texto.manager.analyzer;

import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author oakgen
 */
public class StanzaConlluAnalyzer extends ConlluAnalyzer {

    @Override
    protected String name() {
        return "conllu-stanza";
    }

    @Override
    public List<String> getConllu(Resource resource, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        String lang = parameters.get("lang");
        if (lang == null) {
            lang = environment.getProperty("analysis.default-lang", "it");
        }
        String stanzaPath = environment.getProperty("analysis.stanza-path", "~/stanza_resources");
        if (stanzaPath.startsWith("~/")) {
            String home = environment.getProperty("user.home");
            stanzaPath = home + stanzaPath.substring(1);
        }
        if (!stanzaPath.endsWith("/")) {
            stanzaPath += "/";
        }
        if (!Files.exists(Path.of(stanzaPath))) {
            throw new RuntimeException("stanza not found " + stanzaPath);
        }
        String sql = "select text from _text where resource_id = " + resource.getId();
        String text = databaseManager.queryFirst(sql, String.class);
        Path input = Path.of(stanzaPath, "input_" + resource.getId() + ".txt");
        Path output = Path.of(stanzaPath, "output_" + resource.getId() + ".txt");
        try {
            Files.write(input, text.getBytes());
            int ret = Runtime.getRuntime().exec(new String[]{"python3", stanzaPath + "analyze.py", lang, input.toString(), output.toString()}).waitFor();
            if (ret == 0) {
                return Files.readAllLines(Path.of(stanzaPath, "output.conllu"));
            } else {
                throw new RuntimeException("error execute stanza script");
            }
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                Files.delete(input);
            } catch (IOException e) {
            }
            try {
                Files.delete(output);
            } catch (IOException e) {
            }
        }
    }

}
