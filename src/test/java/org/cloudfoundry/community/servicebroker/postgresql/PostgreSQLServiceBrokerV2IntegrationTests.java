package org.cloudfoundry.community.servicebroker.postgresql;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.cloudfoundry.community.servicebroker.ServiceBrokerV2IntegrationTestBase;

import org.cloudfoundry.community.servicebroker.postgresql.config.BrokerConfiguration;
import org.cloudfoundry.community.servicebroker.postgresql.service.PostgreSQLDatabase;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;

import java.sql.Connection;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringApplicationConfiguration(classes = Application.class)
public class PostgreSQLServiceBrokerV2IntegrationTests extends ServiceBrokerV2IntegrationTestBase {

    @Value("${MASTER_JDBC_URL}")
    private String jdbcUrl;

    private Connection conn;

    @Autowired
    private PostgreSQLDatabase postgreSQLDatabase;

//    @Override
//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//        conn = DriverManager.getConnection(this.jdbcUrl);
//    }

    /**
     * Sanity check, to make sure that the 'service' table required for this Service Broker is created.
     */

    @Test
    public void case0_checkThatServiceTableIsCreated() throws Exception {
        assertTrue(checkTableExists("service"));
    }

    /**
     * cf marketplace
     * cf create-service-broker
     * <p>
     * Fetch Catalog (GET /v2/catalog)
     */

    @Override
    @Test
    public void case1_fetchCatalogSucceedsWithCredentials() throws Exception {
        // same as super code, but we need the response here
        ValidatableResponse response = given().auth().basic(username, password).header(apiVersionHeader).when().get(fetchCatalogPath).then().statusCode(HttpStatus.SC_OK);

        BrokerConfiguration brokerConfiguration = new BrokerConfiguration();

        ServiceDefinition serviceDefinition = brokerConfiguration.catalog().getServiceDefinitions().get(0);

        response.body("services[0].id", equalTo(serviceDefinition.getId()));
        response.body("services[0].name", equalTo(serviceDefinition.getName()));
        response.body("services[0].description", equalTo(serviceDefinition.getDescription()));
        response.body("services[0].requires", equalTo(serviceDefinition.getRequires()));
        response.body("services[0].tags", equalTo(serviceDefinition.getTags()));

        List<String> planIds = serviceDefinition.getPlans().stream().map(Plan::getId).collect(Collectors.toList());
        response.body("services[0].plans.id", equalTo(planIds));
    }

    /**
     * cf create-service
     * <p>
     * Provision Instance (PUT /v2/service_instances/:id)
     */

    @Override
    @Test
    public void case2_provisionInstanceSucceedsWithCredentials() throws Exception {
        super.case2_provisionInstanceSucceedsWithCredentials();

        assertTrue(checkDatabaseExists(instanceId));
        assertTrue(checkRoleExists(instanceId));
        assertTrue(checkRoleIsDatabaseOwner(instanceId, instanceId));

        Map<String, String> serviceResult = postgreSQLDatabase.executePreparedSelect("SELECT * FROM service WHERE serviceinstanceid = ?", ImmutableMap.of(1, instanceId));
        assertThat(serviceResult.get("organizationguid"), is(organizationGuid));
        assertThat(serviceResult.get("planid"), is(planId));
        assertThat(serviceResult.get("spaceguid"), is(spaceGuid));
        assertThat(serviceResult.get("servicedefinitionid"), is(serviceId));
        assertThat(serviceResult.get("serviceinstanceid"), is(instanceId));
    }

    /**
     * cf bind-service
     * <p>
     * Create Binding (PUT /v2/service_instances/:instance_id/service_bindings/:id)
     */

    @Override
    @Test
    public void case3_createBindingSucceedsWithCredentials() throws Exception {
        // same as super code, but we need the response here
        String createBindingPath = String.format(createOrRemoveBindingBasePath, instanceId, serviceId);
        String request_body = "{\n" +
                "  \"plan_id\":      \"" + planId + "\",\n" +
                "  \"service_id\":   \"" + serviceId + "\",\n" +
                "  \"app_guid\":     \"" + appGuid + "\"\n" +
                "}";

        ValidatableResponse response = given().auth().basic(username, password).header(apiVersionHeader).request().contentType(ContentType.JSON).body(request_body).when().put(createBindingPath).then().statusCode(HttpStatus.SC_CREATED);

        response.body("credentials.uri", containsString("postgres://" + instanceId));
        response.body("syslog_drain_url", is(nullValue()));
    }

    /**
     * cf unbind-service
     * <p>
     * Remove Binding (DELETE /v2/service_instances/:instance_id/service_bindings/:id)
     */

    @Override
    @Test
    public void case4_removeBindingSucceedsWithCredentials() throws Exception {
        // same as super code, but we need the response here
        String removeBindingPath = String.format(createOrRemoveBindingBasePath, instanceId, serviceId) + "?service_id=" + serviceId + "&plan_id=" + planId;
        ValidatableResponse response = given().auth().basic(username, password).header(apiVersionHeader).when().delete(removeBindingPath).then().statusCode(HttpStatus.SC_OK);

        // response body is empty json
        response.body(equalTo("{}"));
    }

    /**
     * cf delete-service
     * <p>
     * Remove Instance (DELETE /v2/service_instances/:id)
     */

    @Override
    @Test
    public void case5_removeInstanceSucceedsWithCredentials() throws Exception {
        super.case5_removeInstanceSucceedsWithCredentials();

        assertFalse(checkDatabaseExists(instanceId));
        assertFalse(checkRoleExists(instanceId));
        assertFalse(checkRoleIsDatabaseOwner(instanceId, instanceId));

        Map<String, String> serviceResult = postgreSQLDatabase.executePreparedSelect("SELECT * FROM service WHERE serviceinstanceid = ?", ImmutableMap.of(1, instanceId));
        assertTrue(serviceResult.isEmpty());
    }

    private boolean checkTableExists(String tableName) throws Exception {

        Map<String,String> result = postgreSQLDatabase.executePreparedSelect("select tablename from pg_catalog.pg_tables where tablename = ?", ImmutableMap.of(1, tableName));

        return result.size() == 1;
    }

    private boolean checkDatabaseExists(String databaseName) throws Exception {
        Map<String, String> pgDatabaseResult = postgreSQLDatabase.executePreparedSelect("SELECT * FROM pg_catalog.pg_database WHERE datname = ?", ImmutableMap.of(1, databaseName));
        return pgDatabaseResult.size() > 0;
    }

    private boolean checkRoleExists(String roleName) throws Exception {
        Map<String, String> pgRoleResult = postgreSQLDatabase.executePreparedSelect("SELECT * FROM pg_catalog.pg_roles WHERE rolname = ?", ImmutableMap.of(1, roleName));
        return pgRoleResult.size() > 0;
    }

    private boolean checkRoleIsDatabaseOwner(String roleName, String databaseName) throws Exception {
        Map<String, String> pgRoleIsDatabaseOwnerResult = postgreSQLDatabase.executePreparedSelect("SELECT d.datname as name, " +
                "pg_catalog.pg_get_userbyid(d.datdba) as owner FROM pg_catalog.pg_database d WHERE d.datname = ?", ImmutableMap.of(1, databaseName));
        String owner = pgRoleIsDatabaseOwnerResult.get("owner");
        return (owner != null) ? owner.equals(roleName) : false;
    }
}