# Oxalate portal application

## Database

## Security

Oxalate portal uses standard Spring boot security mechanism.

### Brute force mitigation

To mitigate brute force attack on the login endpoint, the application has an event-listener that listens to the `AuthenticationFailureBadCredentialsEvent`
event. When this event is triggered, the application will check if the user has exceeded the maximum number of attempts allowed, currently 10. If so, the
IP address of the request be blocked for a period of 1 day. The attempts are stored in an in-memory cache, so the application will not remember the attempts
after restart.

See [`AuthenticationFailureListener`](../service/src/main/java/io/oxalate/backend/security/AuthenticationFailureListener.java) and
[`LoginAttemptService`](../service/src/main/java/io/oxalate/backend/security/LoginAttemptService.java) classes for more details.
