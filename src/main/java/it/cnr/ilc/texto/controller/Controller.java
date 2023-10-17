package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.manager.AccessManager;
import it.cnr.ilc.texto.manager.exception.ManagerException;
import it.cnr.ilc.texto.manager.exception.AuthorizationException;
import it.cnr.ilc.texto.manager.exception.ForbiddenException;
import it.cnr.ilc.texto.manager.DatabaseManager;
import it.cnr.ilc.texto.manager.LogManager;
import it.cnr.ilc.texto.manager.MonitorManager;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 *
 * @author oakgen
 */
public abstract class Controller {

    @Autowired
    protected Environment environment;
    @Autowired
    protected AccessManager accessManager;
    @Autowired
    protected MonitorManager monitorManager;
    @Autowired
    protected LogManager logManager;
    @Autowired
    protected DatabaseManager databaseManager;

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<String> handleException(AuthorizationException exception) throws SQLException {
        databaseManager.rollbackAndReleaseConnection();
        logManager.appendMessage(exception.getMessage());
        return new ResponseEntity(exception.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleException(ForbiddenException exception) throws SQLException {
        databaseManager.rollbackAndReleaseConnection();
        logManager.appendMessage(exception.getMessage());
        return new ResponseEntity(exception.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<String> handleException(InterruptedException exception) throws SQLException {
        databaseManager.rollbackAndReleaseConnection();
        logManager.appendMessage("interrupted");
        return new ResponseEntity(exception.getMessage(), HttpStatus.RESET_CONTENT);
    }

    @ExceptionHandler(ManagerException.class)
    public ResponseEntity<String> handleException(ManagerException exception) throws SQLException {
        databaseManager.rollbackAndReleaseConnection();
        logManager.appendMessage(exception.getMessage());
        return new ResponseEntity(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
