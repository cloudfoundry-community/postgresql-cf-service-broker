package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private final Connection conn;

    @Autowired
    public Database(Connection conn) {
        this.conn = conn;
    }

    public void createDatabaseForInstance(String instanceId, String serviceId, String planId, String organizationGuid, String spaceGuid) throws SQLException {
        Utils.checkValidUUID(instanceId);

        Statement statement = this.conn.createStatement();

        PreparedStatement insertService = this.conn.prepareStatement("INSERT INTO service (serviceinstanceid, servicedefinitionid, planid, organizationguid, spaceguid) VALUES (?, ?, ?, ?, ?)");
        insertService.setString(1, instanceId);
        insertService.setString(2, serviceId);
        insertService.setString(3, planId);
        insertService.setString(4, organizationGuid);
        insertService.setString(5, spaceGuid);

        try {
            statement.execute("CREATE DATABASE \"" + instanceId + "\" ENCODING 'UTF8'");
            statement.execute("REVOKE all on database \"" + instanceId + "\" from public");
            insertService.executeUpdate();
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            statement.close();
            insertService.close();
        }
    }

    public void deleteDatabase(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);

        Statement statement = this.conn.createStatement();

        PreparedStatement getCurrentUser = this.conn.prepareStatement("SELECT current_user");

        PreparedStatement terminateConnections = this.conn.prepareStatement("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()");
        terminateConnections.setString(1, instanceId);

        PreparedStatement deleteService = this.conn.prepareStatement("DELETE FROM service WHERE serviceinstanceid=?");
        deleteService.setString(1, instanceId);

        try {
            String currentUser = "";
            ResultSet result = getCurrentUser.executeQuery();
            if(result.next()) {
                currentUser = result.getString("current_user");
            } else {
                logger.warn("Current user could not be found?");
            }
            terminateConnections.executeQuery();
            statement.execute("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + currentUser + "\"");
            statement.execute("DROP DATABASE \"" + instanceId + "\"");
            deleteService.executeUpdate();
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            statement.close();
            deleteService.close();
        }
    }

    public ServiceInstance findServiceInstance(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);
        PreparedStatement findDatabase = this.conn.prepareStatement("SELECT * FROM service WHERE serviceinstanceid = ?");
        findDatabase.setString(1, instanceId);

        ResultSet result = null;
        try {
            result = findDatabase.executeQuery();
            if (result.next()) {
                String serviceDefinitionId = result.getString("servicedefinitionid");
                String organizationGuid = result.getString("organizationguid");
                String planId = result.getString("planid");
                String spaceGuid = result.getString("spaceguid");

                return new ServiceInstance(instanceId, serviceDefinitionId, planId, organizationGuid, spaceGuid, null);
            }
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            findDatabase.close();
            if (result != null)
                result.close();
        }
        return null;
    }

    public List<ServiceInstance> getAllServiceInstances() {
        List<ServiceInstance> serviceInstances = Lists.newArrayList();
        return serviceInstances;
    }
}
