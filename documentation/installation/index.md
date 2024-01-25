# Installation of Oxalate backend

*NOTE!* These instructions will for the moment only cover how to set up and run the backend service in a Linux environment whether natively or in a container.

Oxalate backend is a java spring boot application. The most common way to run this application is to run it as a docker container. In all cases you also need
a database (PostgreSQL) to store the data. The easiest way to set up the database is to run it as a docker container as well.

## Setting up the database

### As a part of the docker compose setup

This will require no additional changes, you only need to set values in the docker-compose.yaml file.

### As a standalone docker container

This will require some additional configuration. If you're only doing a local try-out and run the backend application natively, then the recommended way to set
the database up is running the [`db_local_setup.sh`](../../db_local_setup.sh) script which you can find in the root of this repository.

This will set up a PostgreSQL 15 container for you.

```bash
./db_local_setup.sh
```

Be aware that this will wipe out any previous container and volume called `dev_oxdb` and `oxdb_volume` respectively.

### Pre-existing natively running database

If you have a pre-existing database running natively, then you need to add a user and a database for the Oxalate backend application. The database should
be called `oxdb`. If you're only doing a local try-out, then you can use the `postgres` user, but if you're running the application in production, then you
should create a separate user for the application.

### Populating database with test data

If your purpose is to try out the application, then you can populate the database with test data present in the [`test_data.sql`](../../test_data.sql) file.
Assuming that you're using the `postgres` user, you can populate the database with the following command (assuming that you're in the root of the repository):

```bash
cat test-data/test-large.sql  | psql --host=localhost --port=5432 --username postgres oxdb
```

This will generate a number of users with various roles. The credentials are listed in the main [README.md](../../README.md) file.

## Run backend service from ready-made docker image

The docker image is available on GitHub (ghcr.io). To download the image to your local docker, issue the following command:

```bash
docker image pull ghcr.io/oxalate-portal/oxalate-backend:latest
```

## Build and run locally

One can also build the application locally and run it natively or in a self-built docker container. For this you need to take the following steps:

1. Install Java 21 JDK
2. Install git (most distributions have this pre-installed)
3. Clone the repository with the following command to a directory of your choice:
   ```bash
   git clone git@github.com:Oxalate-Portal/oxalate-backend.git
   ```
4. Create a local Spring Boot profile in the `service/src/main/resources` directory called `local.properties`. You can use the `local.yaml.template` found in
   the [templates](../../templates) directory as a starting point. You need to set all of the bracketed properties in the file. In case you also have set up
   the database in any other way than running the shell script, then you probably need to modify the properties under `spring.datasource` and `spring.flyway`.
5. Build and run the application with the following command:
   ```bash
   ./mvnw clean verify spring-boot:run -Doxalate.captcha.enabled=false -Dspring.profiles.active=local | tee /tmp/oxalateb.log
   ```

The docker image is built using the Dockerfile in the root of the project. The docker image is built using the following command:

## Google Captcha setup
