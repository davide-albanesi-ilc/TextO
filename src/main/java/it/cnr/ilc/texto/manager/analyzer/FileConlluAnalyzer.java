package it.cnr.ilc.texto.manager.analyzer;

import it.cnr.ilc.texto.domain.Resource;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author oakgen
 */
public class FileConlluAnalyzer extends ConlluAnalyzer {

    @Override
    protected String name() {
        return "conllu-file";
    }

    @Override
    public List<String> getConllu(Resource resource, Map<String, String> parameters) throws SQLException, ReflectiveOperationException, ManagerException {
        String fileName = parameters.get("file");
        if (fileName == null) {
            throw new ManagerException("parameter file missing");
        }
        try {
            return Files.readAllLines(Paths.get(fileName));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
