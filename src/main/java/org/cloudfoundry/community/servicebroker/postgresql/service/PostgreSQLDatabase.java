package org.cloudfoundry.community.servicebroker.postgresql.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class PostgreSQLDatabase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLDatabase.class);

    private static Connection conn;

    private static String databaseHost;

    private static int databasePort;

    @Autowired
    public PostgreSQLDatabase(Connection conn) {
        PostgreSQLDatabase.conn = conn;

        try {
            String jdbcUrl = conn.getMetaData().getURL();
            // Remove "jdbc:" prefix from the connection JDBC URL to create an URI out of it.
            String cleanJdbcUrl = jdbcUrl.replace("jdbc:", "");

            URI uri = new URI(cleanJdbcUrl);
            PostgreSQLDatabase.databaseHost = uri.getHost();
            PostgreSQLDatabase.databasePort = uri.getPort() == -1 ? 5432 : uri.getPort();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to get DatabaseMetadata from Connection", e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to parse JDBC URI for Database Connection", e);
        }
    }


    public static void executeUpdate(String query) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            statement.execute(query);
        } catch (SQLException e) {
            logger.error("Error while executing SQL UPDATE query '" + query + "'", e);
        } finally {
            statement.close();
        }
    }

    public static Map<String, String> executeSelect(String query) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            ResultSet result = statement.executeQuery(query);
            return getResultMapFromResultSet(result);
        } catch (SQLException e) {
            logger.error("Error while executing SQL SELECT query '" + query + "'", e);
            return null;
        }
    }

    public static void executePreparedUpdate(String query, Map<Integer, String> parameterMap) throws SQLException {
        if(parameterMap == null) {
            throw new IllegalStateException("parameterMap cannot be null");
        }

        PreparedStatement preparedStatement = conn.prepareStatement(query);

        for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
            preparedStatement.setString(parameter.getKey(), parameter.getValue());
        }

        try {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error while executing SQL prepared UPDATE query '" + query + "'", e);
        } finally {
            preparedStatement.close();
        }
    }

    public static Map<String, String> executePreparedSelect(String query, Map<Integer, String> parameterMap) throws SQLException {
        if(parameterMap == null) {
            throw new IllegalStateException("parameterMap cannot be null");
        }

        PreparedStatement preparedStatement = conn.prepareStatement(query);

        for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
            preparedStatement.setString(parameter.getKey(), parameter.getValue());
        }

        try {
            ResultSet result = preparedStatement.executeQuery();
            return getResultMapFromResultSet(result);
        } catch (SQLException e) {
            logger.error("Error while executing SQL prepared SELECT query '" + query + "'", e);
            return null;
        }
    }

    public static String getDatabaseHost() {
        return databaseHost;
    }

    public static int getDatabasePort() {
        return databasePort;
    }

    private static Map<String, String> getResultMapFromResultSet(ResultSet result) throws SQLException {
        ResultSetMetaData resultMetaData = result.getMetaData();
        int columns = resultMetaData.getColumnCount();

        Map<String, String> resultMap = new HashMap<String, String>(columns);

        if(result.next()) {
            for(int i = 1; i <= columns; i++) {
                resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
            }
        }

        return resultMap;
    }
}
