# Conventions

## REST

### URI path form and case

The main resource group (for event, user, etc.) should be in plural form. For example:

```
    /api/users
```

The URI path should be in lower case, and use hyphens to separate words (ie. kebab-case). For example:

```
    /api/users/recover-password
```

### HTTP methods

| Method | Description |
|--------|-------------|
| GET    | Read        |
| POST¹  | Create      |
| PUT    | Update      |
| DELETE | Delete      |
| PATCH² | Update      |

¹ POST is also used when logging in, as it is not a RESTful operation.

² PATCH is not currently used in this project. It may be taken into use in the future for specific use cases.

### HTTP status codes

In order to minimize data leaking to the client, the server should always return the least descriptive status code possible. So instead of distinguishing
between "user not found" and "wrong password", the server should always return "invalid credentials". A simple rule is to pick one error code for all error
cases, and then only use HttpStatus.OK for the successful case. All error cases should however be logged in detail (but avoid personal information if possible)
on the server side for investigations.

### Response creation

We should stick to returning the response in the following manner:

```java
    return ResponseEntity.status(HttpStatus.OK).body(someResponseObject);
```

And in the case of an error:

```java
    log.error("Failed to zyx user ID {}: {}", userId, e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
```