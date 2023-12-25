# Codestyle

In Java-code we follow the general practise of declarations typed in Pascal case (ThisIsPascalCase) , and instantiated
variables as Camel case (thisIsCamelCase).

Example:

```java
public class PascalClass {
	private String camelVariable;
}
```

Databases, schemas, tables and fields are named using snake case (this_is_snake_case).

Example:

```sql
CREATE TABLE a_db.the_schema.and_table (
    a_field VARCHAR(128) NOT NULL,
    another_field NUMBER(12) NOT NULL
);
```

JSON objects should have their fields in snake case (this_is_snake_case).

Example:

```json
{
  "a_field": "A value",
  "another_field": "Another value",
  "a_list": ["A", "B", "C"]
}
```

REST URL path should use kebab case (this-is-kebab-case).

Example:

```http request
https://domain/this-is/another-path/to?the-destination=here
```

YAML keys should use kebab case (this-is-kebab-case).

Example:

```yaml
root:
  some-section:
    another-sub-section:
      final: "value"
  another-key: "another value"
```