package it.cnr.ilc.texto.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class ConnectionPool {

    private final String url;
    private final String username;
    private final String password;
    private final Set<Connection> distributed = new HashSet<>();
    private final Stack<Connection> connections = new Stack<>();
    private final String ping;

    public ConnectionPool(String url, String username, String password, String ping) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.ping = ping;
    }

    public Connection createConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(false);
        return connection;
    }

    public synchronized Connection getConnection() throws SQLException {
        Connection connection;
        try {
            connection = connections.pop();
            try (Statement statement = connection.createStatement()) {
                statement.execute(ping);
            } catch (SQLException ex) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
                connection = getConnection();
            }
        } catch (EmptyStackException ex) {
            connection = createConnection();
        }
        distributed.add(connection);
        return connection;
    }

    public synchronized void releaseCommitConnection(Connection connection) throws SQLException {
        if (distributed.remove(connection)) {
            connection.commit();
            connections.add(0, connection);
        }
    }

    public synchronized void releaseRollbackConnection(Connection connection) throws SQLException {
        if (distributed.remove(connection)) {
            connection.rollback();
            connections.add(0, connection);
        }
    }

    public synchronized void closeConnections() {
        for (Connection connection : connections) {
            try {
                connection.close();
            } catch (Exception e) {
            }
        }
        connections.clear();
        for (Connection connection : distributed) {
            try {
                connection.close();
            } catch (Exception e) {
            }
        }
        distributed.clear();
    }

}
