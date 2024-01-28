# Oxalate portal backend

The backend is the service/server part of the Oxalate portal. It is a Spring boot application that provides a REST API for the frontend to consume.

Documentation is also available for:

* [Installation](installation/index.md)
* [Management](management/index.md)

## Database

PostgreSQL 15 is recommended and is the only database that has been tested. Other versions of PostgreSQL may work, but are not guaranteed to work. Using other
databases may work, but will require additional changes to the application code.

## Security

Oxalate portal uses standard Spring boot security mechanism.

### Brute force mitigation

To mitigate brute force attack on the login endpoint, the application has an event-listener that listens to the `AuthenticationFailureBadCredentialsEvent`
event. When this event is triggered, the application will check if the user has exceeded the maximum number of attempts allowed, currently 10. If so, the
IP address of the request be blocked for a period of 1 day. The attempts are stored in an in-memory cache, so the application will not remember the attempts
after restart.

See [`AuthenticationFailureListener`](../service/src/main/java/io/oxalate/backend/security/AuthenticationFailureListener.java) and
[`LoginAttemptService`](../service/src/main/java/io/oxalate/backend/security/LoginAttemptService.java) classes for more details.
