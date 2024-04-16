package it.cnr.ilc.texto.manager;

import it.cnr.ilc.texto.util.ConnectionPool;
import jakarta.annotation.PostConstruct;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class DatabaseManager extends Manager implements Closeable {

    private final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private final Map<Thread, Connection> connectionMap = new ConcurrentHashMap<>();
    private ConnectionPool connectionPool;

    private DatabaseManager() {
    }

    @PostConstruct
    private void initDatabase() throws ClassNotFoundException {
        connectionPool = new ConnectionPool(
                environment.getProperty("database.url"),
                environment.getProperty("database.username"),
                environment.getProperty("database.password"),
                environment.getProperty("database.ping", "select null"));
    }

    @Override
    public void close() {
        connectionPool.closeConnections();
    }

    public Connection createConnection() throws SQLException {
        return connectionPool.createConnection();
    }

    public Connection getConnection() throws SQLException {
        Connection connection = connectionMap.get(Thread.currentThread());
        if (connection == null) {
            connection = connectionPool.getConnection();
            connectionMap.put(Thread.currentThread(), connection);
        }
        return connection;
    }

    public void releaseCommitConnection() throws SQLException {
        Connection connection = connectionMap.remove(Thread.currentThread());
        if (connection != null) {
            connectionPool.releaseCommitConnection(connection);
        }
    }

    public void releaseRollbackConnection() throws SQLException {
        Connection connection = connectionMap.remove(Thread.currentThread());
        if (connection != null) {
            connectionPool.releaseRollbackConnection(connection);
        }
    }

    public List<Map<String, Object>> query(String sql, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            List<Map<String, Object>> list = new ArrayList<>();
            Map<String, Object> record;
            ResultSetMetaData metaData;
            logger.debug(sql.replaceAll("\\s+", " "));
            ResultSet result = statement.executeQuery(sql);
            while (result.next()) {
                metaData = result.getMetaData();
                record = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    record.put(metaData.getColumnLabel(i), result.getObject(i));
                }
                list.add(record);
            }
            return list;
        }
    }

    public List<Map<String, Object>> query(String sql) throws SQLException {
        return query(sql, getConnection());
    }

    public Map<String, Object> queryFirst(String sql, Connection connection) throws SQLException {
        List<Map<String, Object>> list = query(sql, connection);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> queryFirst(String sql) throws SQLException {
        return queryFirst(sql, getConnection());
    }

    public <T> T queryFirst(String sql, Class<T> clazz, Connection connection) throws SQLException {
        List<Map<String, Object>> list = query(sql, connection);
        return list.isEmpty() ? null : (T) list.get(0).values().toArray()[0];
    }

    public <T> T queryFirst(String sql, Class<T> clazz) throws SQLException {
        return queryFirst(sql, clazz, getConnection());
    }

    public int update(String sql, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            logger.debug(sql);
            return statement.executeUpdate(sql);
        }
    }

    public int update(String sql) throws SQLException {
        return update(sql, getConnection());
    }

    public InputStream getInputStream(Connection connection, String sql) throws SQLException {
        return new SqlInputStream(connection, sql);
    }

    public InputStream getInputStream(String sql) throws SQLException {
        return new SqlInputStream(getConnection(), sql);
    }

    private class SqlInputStream extends InputStream {

        private final Statement statement;
        private final InputStream input;

        private SqlInputStream(Connection connection, String sql) throws SQLException {
            statement = connection.createStatement();
            try {
                ResultSet result = statement.executeQuery(sql);
                if (result.next()) {
                    input = result.getBinaryStream(1);
                } else {
                    input = InputStream.nullInputStream();
                }
            } catch (SQLException se) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
                throw se;
            }
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }

        @Override
        public void close() throws IOException {
            try {
                statement.close();
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

    }

}
