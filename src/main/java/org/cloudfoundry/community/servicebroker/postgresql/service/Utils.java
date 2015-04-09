package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static Connection conn;

    @Autowired
    public Utils(Connection conn) {
        Utils.conn = conn;
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
            logger.warn(e.getMessage());
        } finally {
            statement.close();
        }
    }
}
