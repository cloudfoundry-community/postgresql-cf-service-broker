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
package com.mendix.servicebroker.postgresql.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;


@Component
public class Role {

    private final Connection conn;

    @Autowired
    public Role(Connection conn) {
        this.conn = conn;
    }

    public void createDatabaseForInstance(String instanceId, String password) throws SQLException {
        PreparedStatement createDatabase = this.conn.prepareStatement("CREATE DATABASE ?");
        createDatabase.setString(1, instanceId);

        PreparedStatement makePrivate = this.conn.prepareStatement("REVOKE all on database ? from public");
        makePrivate.setString(1, instanceId);

        PreparedStatement createRole = this.conn.prepareStatement("CREATE ROLE ? LOGIN PASSWORD ?");
        createRole.setString(1, instanceId);
        createRole.setString(2, password);

        PreparedStatement grantRole = this.conn.prepareStatement("GRANT ALL ON DATABASE ? TO ?");
        grantRole.setString(1, instanceId);
        grantRole.setString(2, instanceId);

        try {
            this.conn.setAutoCommit(false);
            createDatabase.executeQuery();
            makePrivate.executeQuery();
            createRole.executeQuery();
            grantRole.executeQuery();
            this.conn.commit();
        } catch (SQLException e) {
            this.conn.rollback();
        } finally {
            this.conn.setAutoCommit(true);
        }
    }

    public void deleteDatabase(String instanceId) throws SQLException {
        PreparedStatement deleteDatabase = this.conn.prepareStatement("DROP DATABASE ?");
        deleteDatabase.setString(1, instanceId);

        PreparedStatement deleteRole = this.conn.prepareStatement("DROP ROLE ?");
        deleteRole.setString(1, instanceId);

        try {
            this.conn.setAutoCommit(false);
            deleteRole.executeQuery();
            deleteDatabase.executeQuery();
            this.conn.commit();
        } catch (SQLException e) {
            this.conn.rollback();
        } finally {
            this.conn.setAutoCommit(true);
        }
    }

    public ServiceInstance findServiceInstance(String instanceId) throws SQLException {
        PreparedStatement findDatabase = this.conn.prepareStatement("SELECT datname FROM pg_database WHERE datname = ?");
        ResultSet result = null;
        try {
            result = findDatabase.executeQuery();
            if (result.next())
                return null;
        } catch (SQLException e) {
        } finally {
            if (result != null)
                result.close();
        }
        return null;
    }

    public List<ServiceInstance> getAllServiceInstances() {
        List<ServiceInstance> serviceInstances = Lists.newArrayList();
        return serviceInstances;
    }

    private ServiceInstance createServiceInstance(String instanceId) {
        return null;
    }
}