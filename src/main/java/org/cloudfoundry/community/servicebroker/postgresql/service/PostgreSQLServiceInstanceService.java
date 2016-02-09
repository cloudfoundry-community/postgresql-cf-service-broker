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
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class PostgreSQLServiceInstanceService implements ServiceInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLServiceInstanceService.class);

    private final Database db;

    private final Role role;

    @Autowired
    public PostgreSQLServiceInstanceService(Database db, Role role) {
        this.db = db;
        this.role = role;
    }

    @Override
    public ServiceInstance createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest)
            throws ServiceInstanceExistsException, ServiceBrokerException {
        String serviceInstanceId = createServiceInstanceRequest.getServiceInstanceId();
        String serviceId = createServiceInstanceRequest.getServiceDefinitionId();
        String planId = createServiceInstanceRequest.getPlanId();
        String organizationGuid = createServiceInstanceRequest.getOrganizationGuid();
        String spaceGuid = createServiceInstanceRequest.getSpaceGuid();
        try {
            db.createDatabaseForInstance(serviceInstanceId, serviceId, planId, organizationGuid, spaceGuid);
            role.createRoleForInstance(serviceInstanceId);
        } catch (SQLException e) {
            logger.error("Error while creating service instance '" + serviceInstanceId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }
        return new ServiceInstance(createServiceInstanceRequest);
    }

    @Override
    public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest)
            throws ServiceBrokerException {
        String serviceInstanceId = deleteServiceInstanceRequest.getServiceInstanceId();
        ServiceInstance instance = getServiceInstance(serviceInstanceId);

        try {
            db.deleteDatabase(serviceInstanceId);
            role.deleteRole(serviceInstanceId);
        } catch (SQLException e) {
            logger.error("Error while deleting service instance '" + serviceInstanceId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }
        return instance;
    }

    @Override
    public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest)
            throws ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public ServiceInstance getServiceInstance(String id) {
        try {
            return db.findServiceInstance(id);
        } catch (SQLException e) {
            logger.error("Error while finding service instance '" + id + "'", e);
            return null;
        }
    }
}