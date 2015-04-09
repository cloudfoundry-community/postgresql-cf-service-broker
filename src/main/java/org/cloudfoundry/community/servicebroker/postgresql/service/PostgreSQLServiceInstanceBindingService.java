package org.cloudfoundry.community.servicebroker.postgresql.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PostgreSQLServiceInstanceBindingService implements ServiceInstanceBindingService {
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLServiceInstanceBindingService.class);
    private final Role role;
    @Value("${POSTGRES_HOSTNAME}")
    private String postgresHostname;
    @Value("${POSTGRES_PORT:5432}")
    private int postgresPort;

    @Autowired
    public PostgreSQLServiceInstanceBindingService(Role role) {
        this.role = role;
    }

    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(String bindingId, ServiceInstance serviceInstance,
            String serviceId, String planId, String appGuid) throws ServiceInstanceBindingExistsException,
            ServiceBrokerException {
        String passwd = "";

        try {
            passwd = this.role.bindRoleToDatabase(serviceInstance.getId());
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        }

        String dbURL = String.format("postgres://%s:%s@%s:%d/%s", serviceInstance.getId(), passwd, this.postgresHostname, this.postgresPort, serviceInstance.getId());

        Map<String, Object> credentials = new HashMap<String, Object>();
        credentials.put("uri", dbURL);

        return new ServiceInstanceBinding(bindingId, serviceInstance.getId(), credentials, null, appGuid);
    }

    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(String bindingId, ServiceInstance serviceInstance,
            String serviceId, String planId) throws ServiceBrokerException {
        try {
            this.role.unBindRoleFromDatabase(serviceInstance.getId());
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        }
        return new ServiceInstanceBinding(bindingId, serviceInstance.getId(), null, null, null);
    }

    @Override
    public ServiceInstanceBinding getServiceInstanceBinding(String id) {
        throw new IllegalStateException("Not implemented");
    }
}