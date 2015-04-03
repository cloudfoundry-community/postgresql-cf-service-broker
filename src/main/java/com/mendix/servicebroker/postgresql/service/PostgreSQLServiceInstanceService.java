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

import java.sql.SQLException;
import java.util.List;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostgreSQLServiceInstanceService implements ServiceInstanceService {
    private final Database db;

    @Autowired
    public PostgreSQLServiceInstanceService(Database db) {
        this.db = db;
    }

    @Override
    public ServiceInstance createServiceInstance(ServiceDefinition service, String serviceInstanceId, String planId,
            String organizationGuid, String spaceGuid) throws ServiceInstanceExistsException, ServiceBrokerException {
        try {
			db.createDatabaseForInstance(serviceInstanceId);
		} catch (SQLException e) {
			throw new ServiceBrokerException(e.toString());
		}
        return new ServiceInstance(serviceInstanceId, service.getId(), planId, organizationGuid, spaceGuid, null);
    }

    @Override
    public ServiceInstance deleteServiceInstance(String id, String serviceId, String planId)
            throws ServiceBrokerException {
        ServiceInstance instance = getServiceInstance(id);
        try {
			db.deleteDatabase(serviceId);
		} catch (SQLException e) {
			throw new ServiceBrokerException(e.toString());
		}
        return instance;
    }

    @Override
    public List<ServiceInstance> getAllServiceInstances() {
        return db.getAllServiceInstances();
    }

    @Override
    public ServiceInstance getServiceInstance(String id) {
        try {
			return db.findServiceInstance(id);
		} catch (SQLException e) {
			return null;
		}
    }

}
