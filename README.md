# cyphera-hibernate

[![CI](https://github.com/cyphera-labs/cyphera-hibernate/actions/workflows/ci.yml/badge.svg)](https://github.com/cyphera-labs/cyphera-hibernate/actions/workflows/ci.yml)
[![Security](https://github.com/cyphera-labs/cyphera-hibernate/actions/workflows/codeql.yml/badge.svg)](https://github.com/cyphera-labs/cyphera-hibernate/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

Transparent field-level format-preserving encryption for [Hibernate](https://hibernate.org/). Annotate a field, data is protected on write and accessed on read. Zero boilerplate.

Built on [`io.cyphera:cyphera`](https://central.sonatype.com/artifact/io.cyphera/cyphera) from Maven Central.

## Quick Start

Add the dependency:

```xml
<dependency>
    <groupId>io.cyphera</groupId>
    <artifactId>cyphera-hibernate</artifactId>
    <version>VERSION</version>
</dependency>
```

Add `cyphera.json` to your classpath or `/etc/cyphera/cyphera.json`. Annotate your fields:

```java
@Entity
public class Customer {

    @Id
    private Long id;

    private String name;

    @CypheraProtect("ssn")
    private String ssn;

    @CypheraProtect("credit_card")
    private String creditCard;
}
```

That's it. The Hibernate Integrator auto-discovers `@CypheraProtect` fields at boot. No converter classes, no config classes, no wiring.

- INSERT/UPDATE: `ssn` → `T01i6J-xF-07pX` in the database
- SELECT: `T01i6J-xF-07pX` → `123-45-6789` back to the entity
- Your code only sees plaintext. The database only sees ciphertext.

## Build

```bash
mvn package -DskipTests
```

## Install / Deploy

1. Add the Maven dependency
2. Place `cyphera.json` on classpath or at `/etc/cyphera/cyphera.json` (or set `CYPHERA_POLICY_FILE` env var)
3. Annotate fields with `@CypheraProtect("policy_name")`
4. Done — the Integrator handles the rest via `META-INF/services` auto-discovery

### Column Sizing

Tags add 3 characters. Ensure your columns have room: **existing width + 3**. Or set `tag_enabled: false` in the policy for same-length output.

## How It Works

1. **Boot**: `CypheraIntegrator` auto-discovered via `META-INF/services`. Registers event listeners.
2. **Scan**: On first access, scans entity fields for `@CypheraProtect` and caches field→policy mapping.
3. **Write**: PreInsert/PreUpdate modify the Hibernate state array — database gets ciphertext, entity keeps plaintext.
4. **Read**: PostLoad decrypts fields on the entity after loading.
5. **SDK**: `CypheraHolder` auto-discovers `cyphera.json` if not explicitly configured.

## Alternative Modes

### Explicit `@Type` (full Hibernate control)

```java
@Type(value = CypheraType.class, parameters = @Parameter(name = "policy", value = "ssn"))
private String ssn;
```

### JPA Compatibility (non-Hibernate providers)

```java
@Converter
public class SsnConverter extends io.cyphera.hibernate.compat.CypheraConverter {
    public SsnConverter() { super("ssn"); }
}
```

## Operations

### Policy Configuration

- Auto-discover: `CYPHERA_POLICY_FILE` env → `./cyphera.json` → `/etc/cyphera/cyphera.json`
- Explicit: `CypheraHolder.set(Cyphera.fromFile("path"))` at bootstrap
- Spring Boot: auto-config handles it (if cyphera-spring is also on classpath)

### Troubleshooting

- **"Unknown policy"** — `@CypheraProtect("...")` doesn't match `cyphera.json`
- **Integrator not loading** — check `META-INF/services` is in the JAR
- **No policy file** — ensure `cyphera.json` exists at one of the auto-discover locations

## Policy File

```json
{
  "policies": {
    "ssn": { "engine": "ff1", "key_ref": "my-key", "tag": "T01" },
    "credit_card": { "engine": "ff1", "key_ref": "my-key", "tag": "T02" }
  },
  "keys": {
    "my-key": { "material": "2B7E151628AED2A6ABF7158809CF4F3C" }
  }
}
```

## Future

- SPI extension points: custom PolicyResolver, FieldProcessor, MetadataResolver
- Spring Boot auto-configuration for CypheraHolder
- Hibernate 5 `@TypeDef` compatibility
- Audit logging integration

## License

Apache 2.0 — Copyright 2026 Horizon Digital Engineering LLC
