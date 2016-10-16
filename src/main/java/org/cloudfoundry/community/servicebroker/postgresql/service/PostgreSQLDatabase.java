package org.cloudfoundry.community.servicebroker.postgresql.service;

import org.cloudfoundry.community.servicebroker.postgresql.model.PGServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.ServiceInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostgreSQLDatabase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLDatabase.class);

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public PostgreSQLDatabase(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init(){
        String serviceTable = "CREATE TABLE IF NOT EXISTS service (serviceinstanceid varchar(200) not null default '',"
                + " servicedefinitionid varchar(200) not null default '',"
                + " planid varchar(200) not null default '',"
                + " organizationguid varchar(200) not null default '',"
                + " spaceguid varchar(200) not null default '')";

        jdbcTemplate.execute(serviceTable);
    }

    public void createDatabaseForInstance(String instanceId, String serviceId,
                                          String planId, String organizationGuid, String spaceGuid) throws SQLException {
        Utils.checkValidUUID(instanceId);
        executeUpdate("CREATE DATABASE \"" + instanceId + "\" ENCODING 'UTF8'");
        executeUpdate("REVOKE all on database \"" + instanceId + "\" from public");

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);
        parameterMap.put(2, serviceId);
        parameterMap.put(3, planId);
        parameterMap.put(4, organizationGuid);
        parameterMap.put(5, spaceGuid);

        executePreparedUpdate("INSERT INTO service (serviceinstanceid, servicedefinitionid, planid, " +
                "organizationguid, spaceguid) VALUES (?, ?, ?, ?, ?)", parameterMap);
    }

    public void deleteDatabase(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);

        Map<String, String> result = executeSelect("SELECT current_user");
        String currentUser = null;

        if(result != null) {
            currentUser = result.get("current_user");
        }

        if(currentUser == null) {
            logger.error("Current user for instance '" + instanceId + "' could not be found");
        }

        executePreparedSelect("SELECT pg_terminate_backend(pg_stat_activity.pid) " +
                "FROM pg_stat_activity WHERE pg_stat_activity.datname = ? AND pid <> pg_backend_pid()", parameterMap);
        executeUpdate("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + currentUser + "\"");
        executeUpdate("DROP DATABASE IF EXISTS \"" + instanceId + "\"");
        executePreparedUpdate("DELETE FROM service WHERE serviceinstanceid=?", parameterMap);
    }

    public PGServiceInstance findServiceInstance(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);

        Map<Integer, String> parameterMap = new HashMap<Integer, String>();
        parameterMap.put(1, instanceId);

        Map<String, String> result = executePreparedSelect("SELECT * FROM service WHERE serviceinstanceid = ?", parameterMap);

        String serviceDefinitionId = result.get("servicedefinitionid");
        String organizationGuid = result.get("organizationguid");
        String planId = result.get("planid");
        String spaceGuid = result.get("spaceguid");
        PGServiceInstance serviceInstance = new PGServiceInstance();
        serviceInstance.setServiceInstanceId(serviceDefinitionId);
        serviceInstance.setOrganizationGuid(organizationGuid);
        serviceInstance.setPlanId(planId);
        serviceInstance.setSpaceGuid(spaceGuid);
        return serviceInstance;
    }

    // TODO needs to be implemented
    public List<PGServiceInstance> getAllServiceInstances() {
        return Collections.emptyList();
    }


    public void createRoleForInstance(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);
        executeUpdate("CREATE ROLE \"" + instanceId + "\"");
        executeUpdate("ALTER DATABASE \"" + instanceId + "\" OWNER TO \"" + instanceId + "\"");
    }

    public void deleteRole(String instanceId) throws SQLException {
        Utils.checkValidUUID(instanceId);
        executeUpdate("DROP ROLE IF EXISTS \"" + instanceId + "\"");
    }

    /**
     *  Binds role to database and returns URI for app connection.
     * @param serviceInstanceId
     * @return
     * @throws Exception
     */
    public String bindRoleToDatabase(String serviceInstanceId) throws Exception {
        Utils.checkValidUUID(serviceInstanceId);

        SecureRandom random = new SecureRandom();
        String passwd = new BigInteger(130, random).toString(32);

        executeUpdate("ALTER ROLE \"" + serviceInstanceId + "\" LOGIN password '" + passwd + "'");

        URI uri = new URI(jdbcTemplate.getDataSource().getConnection().getMetaData().getURL().replace("jdbc:", ""));

        String dbURL = String.format("postgres://%s:%s@%s:%d/%s",
                serviceInstanceId, passwd,
                uri.getHost(), uri.getPort() == -1 ? 5432 : uri.getPort(), serviceInstanceId);

        return dbURL;
    }

    public void unBindRoleFromDatabase(String dbInstanceId) throws SQLException{
        Utils.checkValidUUID(dbInstanceId);
        executeUpdate("ALTER ROLE \"" + dbInstanceId + "\" NOLOGIN");
    }
    /**
     *
     * @param query
     * @throws SQLException
     */
    public void executeUpdate(String query) throws SQLException {

        try {

          jdbcTemplate.execute(query);

        } catch (Exception e) {
            logger.error("Error while executing SQL UPDATE query '" + query + "'", e);
        }

    }

    /**
     *
     * @param query
     * @return
     * @throws SQLException
     */
    public Map<String, String> executeSelect(String query) throws SQLException {

           return jdbcTemplate.query(query, new String[]{}, resultSet -> {
               return processResultSet(resultSet);
           });

    }

    /**
     *
     * @param query
     * @param parameterMap
     * @throws SQLException
     */
    public void executePreparedUpdate(String query, @NotNull Map<Integer, String> parameterMap) throws SQLException {

//        if(parameterMap == null) throw new IllegalStateException("parameterMap cannot be null");

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
                preparedStatement.setString(parameter.getKey(), parameter.getValue());
            }
            return preparedStatement;
        });

    }

    /**
     *
     * @param query
     * @param parameterMap
     * @return
     * @throws SQLException
     */
    public Map<String, String> executePreparedSelect(String query, @NotNull Map<Integer, String> parameterMap)
            throws SQLException {

//        if(parameterMap == null)             throw new IllegalStateException("parameterMap cannot be null");


        return jdbcTemplate.query(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            for(Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
                preparedStatement.setString(parameter.getKey(), parameter.getValue());
            }
            return preparedStatement;
        }, resultSet -> {
            return processResultSet(resultSet);
        });

    }

    /**
     *
     * @param resultSet
     * @return
     * @throws SQLException
     */
    private Map<String, String> processResultSet(ResultSet resultSet) throws SQLException{

        ResultSetMetaData resultMetaData = resultSet.getMetaData();
        int columns = resultMetaData.getColumnCount();

        Map<String, String> resultMap = new HashMap<>(columns);

        if(resultSet.next()) {
            for(int i = 1; i <= columns; i++) {
                resultMap.put(resultMetaData.getColumnName(i), resultSet.getString(i));
            }
        }
        return resultMap;

    }

}
