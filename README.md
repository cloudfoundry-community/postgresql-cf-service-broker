# Cloud Foundry Service Broker for a PostgreSQL instance

A Cloud Foundry Service Broker for a PostgreSQL instance built based on [s3-cf-service-broker](https://github.com/cloudfoundry-community/s3-cf-service-broker).

The broker currently publishes a single service and plan for provisioning PostgreSQL databases.

## Design 

The broker uses a PostgreSQL table for it's meta data. It does not maintain an internal database so it has no dependencies besides PostgreSQL.

Capability with the Cloud Foundry service broker API is indicated by the project version number. For example, version 2.3.0 is based off the 2.3 version of the broker API.

## Running

Simply run the JAR file and provide a PostgreSQL jdbc url via the `MASTER_JDBC_URL` environment variable.

### Locally

```
mvn package && MASTER_JDBC_URL=jdbcurl java -jar target/postgresql-cf-service-broker-2.3.0-SNAPSHOT.jar
```

### In Cloud Foundry

Build the package with `mvn package` then push it out:
```
cf push postgresql-cf-service-broker -p target/postgresql-cf-service-broker-2.3.0-SNAPSHOT.jar --no-start
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
cf create-service-broker postgresql-cf-service-broker user mysecret http://postgresql-cf-service-broker.cfapps.io
```

Add service broker to Cloud Foundry Marketplace:
```
cf enable-service-access PostgreSQL -p "Basic PostgreSQL Plan" -o ORG
```

## Using the services in your application

### Format of Credentials

The credentials provided in a bind call have the following format:

```
"credentials":{
	"uri":"postgres://__username__:__password__@__hostname__:__port__/__database__"
}
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

