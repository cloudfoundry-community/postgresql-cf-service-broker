package org.cloudfoundry.community.servicebroker.postgresql.service;

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
    private final Role role;

    @Autowired
    public PostgreSQLServiceInstanceService(Database db, Role role) {
        this.db = db;
        this.role = role;
    }

    @Override
    public ServiceInstance createServiceInstance(ServiceDefinition service, String serviceInstanceId, String planId,
            String organizationGuid, String spaceGuid) throws ServiceInstanceExistsException, ServiceBrokerException {
        try {
            db.createDatabaseForInstance(serviceInstanceId, service.getId(), planId, organizationGuid, spaceGuid);
            role.createRoleForInstance(serviceInstanceId);
        } catch (SQLException e) {
            throw new ServiceBrokerException(e.getMessage());
        }
        return new ServiceInstance(serviceInstanceId, service.getId(), planId, organizationGuid, spaceGuid, null);
    }

    @Override
    public ServiceInstance deleteServiceInstance(String id, String serviceId, String planId)
            throws ServiceBrokerException {
        ServiceInstance instance = getServiceInstance(id);

        try {
            db.deleteDatabase(id);
            role.deleteRole(id);
        } catch (SQLException e) {
            throw new ServiceBrokerException(e.getMessage());
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