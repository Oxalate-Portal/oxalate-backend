<!--ts-->

* [Installation of Oxalate backend](#installation-of-oxalate-backend)
    * [Setting up the database](#setting-up-the-database)
        * [As a part of the docker compose setup](#as-a-part-of-the-docker-compose-setup)
        * [As a standalone docker container](#as-a-standalone-docker-container)
        * [Pre-existing natively running database](#pre-existing-natively-running-database)
        * [Populating database with test data](#populating-database-with-test-data)
    * [Run backend service from ready-made docker image](#run-backend-service-from-ready-made-docker-image)
    * [Build and run locally](#build-and-run-locally)
        * [Build and run backend service in a docker container](#build-and-run-backend-service-in-a-docker-container)
            * [Running the backend service with docker-compose](#running-the-backend-service-with-docker-compose)
            * [Running the backend service with docker](#running-the-backend-service-with-docker)
    * [Google Captcha setup](#google-captcha-setup)

<!-- Created by https://github.com/ekalinin/github-markdown-toc -->
<!-- Added by: poltsi, at: Thu Jan 25 08:46:15 PM EET 2024 -->

<!--te-->

# Installation of Oxalate backend

**NOTE!** These instructions will for the moment only cover how to set up and run the backend service in a Linux environment whether natively or in a container.

Oxalate backend is a java spring boot application. The most common way to run this application is to run it as a docker container. In all cases you also need
a database (PostgreSQL) to store the data. The easiest way to set up the database is to run it as a docker container as well.

## Setting up the primary administrator

The first time you start the backend service, you need to override the following configuration variables:

```properties
oxalate.first-time=true
oxalate.admin.username=<your username>
oxalate.admin.hashed-password=<your hashed password string>
```

Note that the password is a hashed string. You can generate a bcrypted password with the following command (from Apache httpd-utils):

```bash
htpasswd -bnBC 10 "" yourpassword  | tr -d ':'
```

If you put the hashed password into the docker compose-file, then remember to use double dollar signs to get a literal `$`.

## Setting up the database

### As a part of the docker compose setup

This will require no additional changes, you only need to set values in the [docker-compose.yaml](../../templates/docker-compose.yaml.j2) file. If you're
starting up the backend service for the first time, then remember to add the primary administrator credentials as described in the previous section.

### As a standalone docker container

This will require some additional configuration. If you're only doing a local try-out and run the backend application natively, then the recommended way to set
the database up is running the [`db_local_setup.sh`](../../db_local_setup.sh) script which you can find in the root of this repository.

This will set up a PostgreSQL 15 container for you.

```bash
./db_local_setup.sh
```

Be aware that this will wipe out any previous container and volume called `dev_oxdb` and `oxdb_volume` respectively.

If however you intend to run the backend container as a standalone in production, then you need to also set up the primary administrator credentials as
described in the previous section by adding the necessary environment variables to the `docker run` command.

### Pre-existing natively running database

If you have a pre-existing database running natively, then you need to add a user and a database for the Oxalate backend application. The database should
be called `oxdb`. If you're only doing a local try-out, then you can use the `postgres` user, but if you're running the application in production, then you
should create a separate user for the application.

### Populating database with test data

If your purpose is to try out the application, then you can populate the database with test data present in the [`test_data.sql`](../../test_data.sql) file.
**NOTE!** This requires that you have first created the schema by starting up the backend service (see section below).
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
5. Build the application with the following command:
   ```bash
   ./mvnw clean verify package | tee /tmp/oxalateb.log
   ```
   The last part of the command will pipe the output to a log file in `/tmp/oxalateb.log` so that you can inspect it later if needed.
6. If you want to also run the backend service natively then you should first have the database running (see e.g. the `db_local_setup.sh`) before starting
   the backend service up with the following command:
   ```bash
   ./mvnw clean verify spring-boot:run -Doxalate.captcha.enabled=false -Dspring.profiles.active=local | tee /tmp/oxalateb.log
   ```
   Once the service is running, then you can continue by populating the database with test data as described in the previous section. After that you need to
   start up the frontend. See the [frontend documentation](https://github.com/Oxalate-Portal/oxalate-frontend/documentation/setup/). Again, if this is the first
   time then add the primary administrator credentials as described
   previously.

If instead you want to run the backend service in a docker container, then you should stop in step 5 above and continue with the following section.

### Build and run backend service in a docker container

Once you have built the backend service packages (located in `service/target/`, you can create a docker image with the following command:

```bash
docker build -t oxalate-backend:latest .
```

This will use the `Dockerfile` located in the root of the repository to build the image. Verify that you have the built docker image in your local docker
registry with the following command:

```bash
docker image ls
```

You should see something like the following:

```bash
REPOSITORY            TAG       IMAGE ID       CREATED         SIZE
oxalate-backend       latest    fd07817968f6   3 seconds ago   276MB
```

Now you have the option to either start up the backend with the docker-compose file or by running the docker image directly. The former is the recommended.

#### Running the backend service with docker-compose

The easiest way to run the backend service locally in a docker container is to use the docker-compose file. This will also start up the database for you.
To do this, you need to copy the `docker-compose.yaml.j2` file from the [templates](../../templates) directory to an empty directory and rename it
to `docker-compose.yaml`. Note that the file is a Jinja2 template, which should be helpful if you want to use the file later in an Ansible setup.
For local use you need to replace the following bracketed values with the correct values for your environment:

| Variable                        | Description                | Example      |
|---------------------------------|----------------------------|--------------|
| item.ext_app_port               | External application port  | 8080         |
| item.hostname                   | Hostname                   | localhost    |
| item.mail_enabled               | Mail sending enabled       | true         |
| item.name                       | Environment type           | dev          |
| oxalate_admin_email             | Administrator email        | admin@tld    |
| oxalate_admin_password          | Administrator password     | password     |
| oxalate_app_jwt_secret          | JWT secret                 | secret       |
| oxalate_app_org_name            | Organization name          | MyOrg        |
| oxalate_captcha_enabled         | Captcha enabled            | true         |
| oxalate_captcha_secret_key      | Google Captcha secret key  | secretkey    |
| oxalate_captcha_site_key        | Google Captcha site key    | sitekey      |
| oxalate_db_name                 | Database name              | oxdb         |
| oxalate_db_password             | Database password          | postgres     |
| oxalate_db_user                 | Database user              | postgres     |
| oxalate_installation_first_time | First time installation    | true         |
| oxalate_language_default        | Default language           | en           |
| oxalate_mail_host               | Mail server host           | mail.tld     |
| oxalate_mail_org_email          | Organization email address | info@tld     |
| oxalate_mail_password           | Mail server password       | password     |
| oxalate_mail_support_email      | Support email address      | support@tld  |
| oxalate_mail_system_email       | System email address       | no-reply@tld |
| oxalate_mail_username           | Mail server username       | user         |

The variables beginning with `item` are used when you set up multiple environments with the same `docker-compose.yaml` file. The other variables are used to
set the general configuration of the backend service. The `oxalate_admin_email` and `oxalate_admin_password` are used to set up the primary administrator and
are required to be non-empty when the `oxalate_installation_first_time` is set to `true`.

If you want to use the locally built docker image, then you need to change the `image` property in the `backend` service to `oxalate-backend:latest` on line 10.
Otherwise you will need to set the environment variable `OXALATE_BACKEND_VERSION` in order to pull the image from the GitHub container registry. Visit the
[package page](https://github.com/Oxalate-Portal/oxalate-backend/pkgs/container/oxalate-portal%2Foxalate-backend) to see which version is the latest.

**Notes:**

* The `item.ext_app_port` should be 8080, unless you want to run the backend service on a different port, in which case you need to remember this when
  configuring the frontend.
* Make sure the database connection url and credentials match the ones you have set up for the database.
* You can turn off the Captcha by setting the `oxalate_captcha_enabled` property to `false`.

Once you have the `docker-compose.yaml` file set up, you can start the backend service with the following command:

```bash
OKS_BACKEND_VERSION="0.9.0" docker compose -f docker-compose.yaml up -d
```

The first part is setting the version of the backend service image. This is used to refer to the version tag of the docker image. The second part is the
command to start the service. If you have modified the image-line in the `docker-compose.yaml` file, then you don't need to set the version here.

#### Running the backend service with docker

This option is only recommended if you want to run the backend service in a container with the database running natively. In this case you need to have the
database running and the credentials set up before starting the backend service. You can start the backend service with the following command:

```bash
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e OXALATE_CAPTCHA_ENABLED=false \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/oxdb \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres oxalate-backend:latest
```

**Note!** The above command mixes both using the `local.yaml`-profile as well as setting some values from the environment just to give you an example of how to
do it either way. The preferred way is to use the `local.yaml`-profile as the list of variables that needs to be set is quite long.

## Google Captcha setup

Google Captcha v3 is used to prevent bots from registering to the service. The captcha is enabled by default, but you can disable it by setting the property
`oxalate.captcha.enabled` to `false` either in the `local.yaml` or as a parameter in the `docker-compose.yaml`. If you want to enable the captcha, then you
need to set up a Google Captcha v3 site and secret key. You can do this by going to the
[Google reCAPTCHA admin page](https://www.google.com/recaptcha/admin/create) and follow the instructions there. If you only intend to run the service locally,
then you can use the `localhost` domain. In this case consider just turning the captcha off.

Once you have the keys, you need to set the following properties in the `local.yaml` file:

```yaml
oxalate:
    captcha:
        enabled: true
        site-key: <your site key>
        secret-key: <your secret key>
```
