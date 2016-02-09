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

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PostgreSQLServiceInstanceBindingService implements ServiceInstanceBindingService {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLServiceInstanceBindingService.class);

    private final Role role;

    @Autowired
    public PostgreSQLServiceInstanceBindingService(Role role) {
        this.role = role;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest)
            throws ServiceInstanceBindingExistsException, ServiceBrokerException {
        String bindingId = createServiceInstanceBindingRequest.getBindingId();
        String serviceInstanceId = createServiceInstanceBindingRequest.getServiceInstanceId();
        String appGuid = createServiceInstanceBindingRequest.getAppGuid();
        String passwd = "";

        try {
            passwd = this.role.bindRoleToDatabase(serviceInstanceId);
        } catch (SQLException e) {
            logger.error("Error while creating service instance binding '" + bindingId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }

        String dbURL = String.format("postgres://%s:%s@%s:%d/%s", serviceInstanceId, passwd, PostgreSQLDatabase.getDatabaseHost(), PostgreSQLDatabase.getDatabasePort(), serviceInstanceId);

        Map<String, Object> credentials = new HashMap<String, Object>();
        credentials.put("uri", dbURL);

        return new ServiceInstanceBinding(bindingId, serviceInstanceId, credentials, null, appGuid);
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest)
            throws ServiceBrokerException {
        String serviceInstanceId = deleteServiceInstanceBindingRequest.getInstance().getServiceInstanceId();
        String bindingId = deleteServiceInstanceBindingRequest.getBindingId();
        try {
            this.role.unBindRoleFromDatabase(serviceInstanceId);
        } catch (SQLException e) {
            logger.error("Error while deleting service instance binding '" + bindingId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }
        return new ServiceInstanceBinding(bindingId, serviceInstanceId, null, null, null);
    }
}