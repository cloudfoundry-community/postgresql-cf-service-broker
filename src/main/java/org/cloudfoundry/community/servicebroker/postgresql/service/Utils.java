package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.sql.SQLException;
import java.util.UUID;

public class Utils {
    public static void checkValidUUID(String instanceId) throws SQLException{
        UUID uuid = UUID.fromString(instanceId);

        if(!instanceId.equals(uuid.toString())) {
            throw new SQLException("UUID '" + instanceId + "' is not an UUID.");
        }
    }
}