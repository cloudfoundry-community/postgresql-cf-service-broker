/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    private static Connection conn;

    private static String databaseHost;

    private static int databasePort;

    @Autowired
    public Utils(Connection conn) throws SQLException {
        Utils.conn = conn;

        try {
            String cleanURI = conn.getMetaData().getURL().substring(5);
            URI uri = URI.create(cleanURI);
            Utils.databaseHost = uri.getHost();
            Utils.databasePort = (uri.getPort() == -1 ? 5432 : uri.getPort());
        } catch (Exception e) {
            throw new SQLException("Unable to get databaseHost and/or databasePort from Connection", e);
        }
    }

    public static void checkValidUUID(String instanceId) throws SQLException{
        UUID uuid = UUID.fromString(instanceId);

        if(!instanceId.equals(uuid.toString())) {
            throw new SQLException("UUID '" + instanceId + "' is not an UUID.");
        }
    }

    public static void executeUpdate(String query) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            statement.execute(query);
        } catch (SQLException e) {
            logger.error(e.toString());
        } finally {
            statement.close();
        }
    }

    public static Map<String, String> executeSelect(String query) throws SQLException {
        Statement statement = conn.createStatement();

        try {
            ResultSet result = statement.executeQuery(query);
            ResultSetMetaData resultMetaData = result.getMetaData();
            int columns = resultMetaData.getColumnCount();

            Map<String, String> resultMap = new HashMap<String, String>(columns);

            if(result.next()) {
                for(int i = 1; i <= columns; i++) {
                    resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
                }
            }

            return resultMap;
        } catch (SQLException e) {
            logger.error(e.toString());
            return null;
        }
    }

    public static void executePreparedUpdate(String query, Map<Integer, String> parameterMap) throws SQLException {
        if(parameterMap == null) {
            throw new SQLException("parameterMap cannot be empty");
        }

        PreparedStatement preparedStatement = conn.prepareStatement(query);

        for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
            preparedStatement.setString(parameter.getKey(), parameter.getValue());
        }

        try {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.toString());
        } finally {
            preparedStatement.close();
        }
    }

    public static Map<String, String> executePreparedSelect(String query, Map<Integer, String> parameterMap) throws SQLException {
        if(parameterMap == null) {
            throw new SQLException("parameterMap cannot be empty");
        }

        PreparedStatement preparedStatement = conn.prepareStatement(query);

        for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
            preparedStatement.setString(parameter.getKey(), parameter.getValue());
        }

        try {
            ResultSet result = preparedStatement.executeQuery();
            ResultSetMetaData resultMetaData = result.getMetaData();
            int columns = resultMetaData.getColumnCount();

            Map<String, String> resultMap = new HashMap<String, String>(columns);

            if(result.next()) {
                for(int i = 1; i <= columns; i++) {
                    resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
                }
            }

            return resultMap;
        } catch (SQLException e) {
            logger.error(e.toString());
            return null;
        }
    }

    public static String getDatabaseHost() {
        return databaseHost;
    }

    public static int getDatabasePort() {
        return databasePort;
    }
}
