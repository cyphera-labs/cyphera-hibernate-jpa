# cyphera-hibernate-jpa

[![CI](https://github.com/cyphera-labs/cyphera-hibernate-jpa/actions/workflows/ci.yml/badge.svg)](https://github.com/cyphera-labs/cyphera-hibernate-jpa/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

Transparent field-level format-preserving encryption for [Hibernate](https://hibernate.org/) and [JPA](https://jakarta.ee/specifications/persistence/) — annotate a field, data is protected on write and accessed on read. Zero code changes to your business logic.

Built on [`io.cyphera:cyphera`](https://central.sonatype.com/artifact/io.cyphera/cyphera) from Maven Central.

## Quick Start

Add the dependency:

```xml
<dependency>
    <groupId>io.cyphera</groupId>
    <artifactId>cyphera-hibernate-jpa</artifactId>
    <version>VERSION</version>
</dependency>
```

Annotate your entity:

```java
@Entity
public class Customer {

    @Id
    private Long id;

    private String name;

    @Convert(converter = SsnConverter.class)
    private String ssn;

    @Convert(converter = CreditCardConverter.class)
    private String creditCard;

    // getters, setters...
}
```

That's it. SSN and credit card are encrypted with FPE on every INSERT/UPDATE and decrypted on every SELECT. Your application code never sees the protected values.

## Build

### From source

```bash
mvn package -DskipTests
```

### Via Docker

```bash
docker build -t cyphera-hibernate-jpa .
```

## Install / Deploy

1. Add the Maven dependency to your project
2. Place `cyphera.json` on the classpath or at `/etc/cyphera/cyphera.json`
3. Set `CYPHERA_POLICY_FILE` env var if using a custom path
4. Use built-in converters or create your own

## Usage

### Built-in Converters

| Converter | Policy | Use Case |
|-----------|--------|----------|
| `SsnConverter` | `ssn` | Social security numbers |
| `CreditCardConverter` | `credit_card` | Credit card numbers |

### Custom Converters

Create a converter for any policy in one line:

```java
@Converter
public class PhoneConverter extends CypheraConverter {
    public PhoneConverter() { super("phone"); }
}
```

Then use it:

```java
@Convert(converter = PhoneConverter.class)
private String phone;
```

### What Happens

```java
// Your code writes plaintext
customer.setSsn("123-45-6789");
repo.save(customer);
// Database stores: "T01i6J-xF-07pX" (tagged, dashes preserved)

// Your code reads plaintext
Customer c = repo.findById(1L);
c.getSsn(); // → "123-45-6789" (transparently accessed)
```

The database column contains protected data. Your application only sees plaintext. Tags are embedded so the converter knows which policy was used.

### Works with any JPA provider

- Hibernate (5.x, 6.x)
- EclipseLink
- Any Jakarta Persistence 3.x compliant provider

## Operations

### Policy Configuration

- Policy file: `/etc/cyphera/cyphera.json` or `CYPHERA_POLICY_FILE` env var
- Or classpath: place `cyphera.json` in `src/main/resources/`
- Policy loaded on first converter use — restart application to reload

### Monitoring

- Converter errors throw `RuntimeException` — surfaces in your application's error handling
- Check application logs for `CypheraLoader` entries at startup

### Upgrading

1. Bump the dependency version in `pom.xml`
2. Rebuild and redeploy

### Troubleshooting

- **"Unknown policy"** — converter references a policy name not in `cyphera.json`
- **"No matching tag"** — database contains data protected with a different policy/tag. Check tag configuration.
- **Null values** — converters pass through nulls safely, no encryption applied
- **Existing unprotected data** — run a migration to protect existing rows: `UPDATE customers SET ssn = cyphera_protect('ssn', ssn)`

### Migrating Existing Data

If you have unprotected data in the database, you need to protect it before enabling the converter. Use a SQL migration with the Cyphera database UDFs (Trino, Postgres, etc.) or a batch script:

```sql
-- Using cyphera-postgres or cyphera-trino
UPDATE customers SET ssn = cyphera_protect('ssn', ssn) WHERE ssn NOT LIKE 'T01%';
```

## Policy File

```json
{
  "policies": {
    "ssn": { "engine": "ff1", "key_ref": "my-key", "tag": "T01" },
    "credit_card": { "engine": "ff1", "key_ref": "my-key", "tag": "T02" },
    "phone": { "engine": "ff1", "key_ref": "my-key", "tag": "T03" }
  },
  "keys": {
    "my-key": { "material": "2B7E151628AED2A6ABF7158809CF4F3C" }
  }
}
```

## Future

- Auto-registration via `@Converter(autoApply = true)` for global field protection
- Spring Boot auto-configuration (detect `cyphera.json` and register converters)
- Audit logging integration
- Batch protect/access for bulk operations
- Support for `javax.persistence` (JPA 2.x) in addition to `jakarta.persistence` (JPA 3.x)

## License

Apache 2.0 — Copyright 2026 Horizon Digital Engineering LLC
