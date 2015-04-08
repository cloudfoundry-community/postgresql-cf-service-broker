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
        Statement createRole = this.conn.createStatement();
        Statement alterDatabase = this.conn.createStatement();

        try {
            createRole.execute("CREATE ROLE \"" + instanceId + "\"");
            alterDatabase.execute("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + instanceId + "\"");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            createRole.close();
            alterDatabase.close();
        }
    }

    public void deleteRole(String instanceId) throws SQLException {
        Statement deleteRole = this.conn.createStatement();

        try {
            deleteRole.execute("DROP ROLE \"" + instanceId + "\"");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            deleteRole.close();
        }
    }

    public String bindRoleToDatabase(String dbInstanceId) throws SQLException {
        checkValidUUID(dbInstanceId);

        SecureRandom random = new SecureRandom();
        String passwd = new BigInteger(130, random).toString(32);

        Statement enableRole = this.conn.createStatement();
        try {
            enableRole.execute("ALTER ROLE \"" + dbInstanceId + "\" LOGIN password '" + passwd + "'");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            enableRole.close();
        }
        return passwd;
    }

    public void unBindRoleFromDatabase(String dbInstanceId) throws SQLException{
        checkValidUUID(dbInstanceId);

        Statement revokeGrant = this.conn.createStatement();

        try {
            revokeGrant.execute("ALTER ROLE \"" + dbInstanceId + "\" NOLOGIN");
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            revokeGrant.close();
        }
    }

    private void checkValidUUID(String instanceId) throws SQLException{
        UUID uuid = UUID.fromString(instanceId);

        if(!instanceId.equals(uuid.toString())) {
            throw new SQLException("UUID '" + instanceId + "' is not an UUID.");
        }
    }
}
