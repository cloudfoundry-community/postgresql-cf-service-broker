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
package org.cloudfoundry.community.servicebroker.postgresql.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.config.BrokerApiVersionConfig;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker", excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = BrokerApiVersionConfig.class) })
public class BrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BrokerConfiguration.class);

    @Value("${MASTER_JDBC_URL}")
    private String jdbcUrl;

    @Bean
    public Connection jdbc() {
        try {
            Connection conn = DriverManager.getConnection(this.jdbcUrl);

            String serviceTable = "CREATE TABLE IF NOT EXISTS service (serviceinstanceid varchar(200) not null default '',"
                    + " servicedefinitionid varchar(200) not null default '',"
                    + " planid varchar(200) not null default '',"
                    + " organizationguid varchar(200) not null default '',"
                    + " spaceguid varchar(200) not null default '')";

            Statement createServiceTable = conn.createStatement();
            createServiceTable.execute(serviceTable);
            return conn;
        } catch (SQLException e) {
            logger.error(e.toString());
            return null;
        }
    }

    @Bean
    public Catalog catalog() throws JsonParseException, JsonMappingException, IOException {
        ServiceDefinition serviceDefinition = new ServiceDefinition("pg", "PostgreSQL",
                "PostgreSQL on shared instance.", true, getPlans(), getTags(), getServiceDefinitionMetadata(),
                Arrays.asList("syslog_drain"), null);
        return new Catalog(Arrays.asList(serviceDefinition));
    }

    private List<String> getTags() {
        return Arrays.asList("PostgreSQL", "Database storage");
    }

    private Map<String, Object> getServiceDefinitionMetadata() {
        Map<String, Object> sdMetadata = new HashMap<String, Object>();
        sdMetadata.put("displayName", "PostgreSQL");
        sdMetadata.put("imageUrl", "https://wiki.postgresql.org/images/3/30/PostgreSQL_logo.3colors.120x120.png");
        sdMetadata.put("longDescription", "PostgreSQL Service");
        sdMetadata.put("providerDisplayName", "PostgreSQL");
        sdMetadata.put("documentationUrl", "http://mendix.com/postgresql");
        sdMetadata.put("supportUrl", "https://support.mendix.com");
        return sdMetadata;
    }

    private List<Plan> getPlans() {
        Plan basic = new Plan("postgresql-basic-plan", "Basic PostgreSQL Plan",
                "A PG plan providing a single database on a shared instance with limited storage.", getBasicPlanMetadata());
        return Arrays.asList(basic);
    }

    private Map<String, Object> getBasicPlanMetadata() {
        Map<String, Object> planMetadata = new HashMap<String, Object>();
        planMetadata.put("bullets", getBasicPlanBullets());
        return planMetadata;
    }

    private List<String> getBasicPlanBullets() {
        return Arrays.asList("Single PG database", "Limited storage", "Shared instance");
    }
}