package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class Role {
    private static final Logger logger = LoggerFactory.getLogger(Role.class);
    private final Connection conn;

    @Autowired
    public Role(Connection conn) {
        this.conn = conn;
    }

    public void createRoleForInstance(String instanceId) throws SQLException {
        Database.checkValidUUID(instanceId);

        Statement statement = this.conn.createStatement();

        try {
            statement.execute("CREATE ROLE \"" + instanceId + "\"");
            statement.execute("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + instanceId + "\"");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            statement.close();
        }
    }

    public void deleteRole(String instanceId) throws SQLException {
        Database.checkValidUUID(instanceId);

        Statement statement = this.conn.createStatement();

        try {
            statement.execute("DROP ROLE \"" + instanceId + "\"");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            statement.close();
        }
    }

    public String bindRoleToDatabase(String dbInstanceId) throws SQLException {
        Database.checkValidUUID(dbInstanceId);

        SecureRandom random = new SecureRandom();
        String passwd = new BigInteger(130, random).toString(32);

        Statement statement = this.conn.createStatement();
        try {
            statement.execute("ALTER ROLE \"" + dbInstanceId + "\" LOGIN password '" + passwd + "'");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            statement.close();
        }
        return passwd;
    }

    public void unBindRoleFromDatabase(String dbInstanceId) throws SQLException{
        Database.checkValidUUID(dbInstanceId);

        Statement statement = this.conn.createStatement();

        try {
            statement.execute("ALTER ROLE \"" + dbInstanceId + "\" NOLOGIN");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            statement.close();
        }
    }
}
