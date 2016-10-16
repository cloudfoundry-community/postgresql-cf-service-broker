# Cloud Foundry Service Broker for a PostgreSQL instance 

The broker currently publishes a single service and plan for provisioning PostgreSQL databases.

## Design 

The broker uses a PostgreSQL table for it's meta data. It does not maintain an internal database so it has no dependencies besides PostgreSQL.

Capability with the Cloud Foundry service broker API is indicated by the project version number. For example, version 2.8.0 is based off the 2.8 version of the broker API.

## Running

Simply run the JAR file and provide a PostgreSQL jdbc url via the `MASTER_JDBC_URL` environment variable.

### Locally

```
mvn package && MASTER_JDBC_URL=jdbcurl java -jar target/postgresql-cf-service-broker-2.8.0-SNAPSHOT.jar
```

### In Cloud Foundry

Find out the database subnet and create a security group rule (postgresql.json):
```
[{"protocol":"tcp","destination":"10.10.8.0/24","ports":"5432"}]
```

import this into CF with:
```
cf create-security-group postgresql-service postgresql.json
```

Bind to the full cf install:
```
cf bind-running-security-group postgresql-service
```


Build the package with `mvn package` then push it out:
```
cf push postgresql-cf-service-broker -p target/postgresql-cf-service-broker-2.8.0-SNAPSHOT.jar --no-start
```

Export the following environment variables:

```
cf set-env postgresql-cf-service-broker MASTER_JDBC_URL "jdbcurl"
cf set-env postgresql-cf-service-broker JAVA_OPTS "-Dsecurity.user.password=mysecret"
```

Start the service broker:
```
cf start postgresql-cf-service-broker
```

Create Cloud Foundry service broker:
```
cf create-service-broker postgresql-cf-service-broker user mysecret http://postgresql-cf-service-broker.bosh-lite.com
```

Add service broker to Cloud Foundry Marketplace:
```
cf enable-service-access PostgreSQL -p "Basic PostgreSQL Plan" -o ORG
```

## Testing

You need to have a running PostgreSQL 9.x instance for this to work locally.
To create an PostgreSQL database matching the ```MASTER_JDBC_URL```. Create the file  ```src/test/resources/application.properties``` and add as below (remember to replace the database name, username and password with the one you use):
```
security.user.password: password
service_id: pg
plan_id: postgresql-basic-plan
MASTER_JDBC_URL: jdbc:postgresql://localhost:5432/db?user=dbuser&password=password
```

Then run:
```
mvn test
```


## Broker Security

[spring-boot-starter-security](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-starters/spring-boot-starter-security) is used. See the documentation here for configuration: [Spring boot security](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-security)

The default password configured is "password"

## Creation of PostgreSQL databases

A service provisioning call will create a PostgreSQL database. A binding call will return a database uri that can be used to connect to the database. Unbinding calls will disable the database user role and deprovisioning calls will delete all resources created.

## User for Broker

An PostgreSQL user must be created for the broker. The username and password must be provided using the environment variable `MASTER_JDBC_URL`.

## Registering a Broker with the Cloud Controller

See [Managing Service Brokers](http://docs.cloudfoundry.org/services/managing-service-brokers.html).

