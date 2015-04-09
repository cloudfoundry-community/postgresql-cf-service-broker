package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.math.BigInteger;
import java.sql.SQLException;
import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class Role {
    public void createRoleForInstance(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);
        Utils.executeUpdate("CREATE ROLE \"" + instanceId + "\"");
        Utils.executeUpdate("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + instanceId + "\"");
    }

    public void deleteRole(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);
        Utils.executeUpdate("DROP ROLE \"" + instanceId + "\"");
    }

    public String bindRoleToDatabase(String dbInstanceId) throws SQLException {
        Utils.checkValidUUID(dbInstanceId);

        SecureRandom random = new SecureRandom();
        String passwd = new BigInteger(130, random).toString(32);

        Utils.executeUpdate("ALTER ROLE \"" + dbInstanceId + "\" LOGIN password '" + passwd + "'");
        return passwd;
    }

    public void unBindRoleFromDatabase(String dbInstanceId) throws SQLException{
        Utils.checkValidUUID(dbInstanceId);
        Utils.executeUpdate("ALTER ROLE \"" + dbInstanceId + "\" NOLOGIN");
    }
}