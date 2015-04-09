package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Utils.executeUpdate("CREATE DATABASE \"" + instanceId + "\" ENCODING 'UTF8'");
        Utils.executeUpdate("REVOKE all on database \"" + instanceId + "\" from public");

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);
        parameterMap.put(2, serviceId);
        parameterMap.put(3, planId);
        parameterMap.put(4, organizationGuid);
        parameterMap.put(5, spaceGuid);

        Utils.executePreparedUpdate("INSERT INTO service (serviceinstanceid, servicedefinitionid, planid, organizationguid, spaceguid) VALUES (?, ?, ?, ?, ?)", parameterMap);
    }

    public void deleteDatabase(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);

        PreparedStatement getCurrentUser = this.conn.prepareStatement("SELECT current_user");

        try {
            String currentUser = "";
            ResultSet result = getCurrentUser.executeQuery();
            if(result.next()) {
                currentUser = result.getString("current_user");
            } else {
                logger.warn("Current user could not be found?");
            }
            Utils.executePreparedUpdate("SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()", parameterMap);
            Utils.executeUpdate("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + currentUser + "\"");
            Utils.executeUpdate("DROP DATABASE \"" + instanceId + "\"");
            Utils.executePreparedUpdate("DELETE FROM service WHERE serviceinstanceid=?", parameterMap);
        } catch (SQLException e) {
            logger.warn(e.getMessage());
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
