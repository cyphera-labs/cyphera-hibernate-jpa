package io.cyphera.hibernate.compat;

import io.cyphera.hibernate.CypheraHolder;
import jakarta.persistence.AttributeConverter;

/**
 * JPA AttributeConverter base class — compatibility mode for non-Hibernate JPA providers.
 *
 * For Hibernate users, prefer the @Type approach (see CypheraType).
 *
 * Usage: subclass with the policy name:
 *   @Converter
 *   public class SsnConverter extends CypheraConverter {
 *       public SsnConverter() { super("ssn"); }
 *   }
 */
public abstract class CypheraConverter implements AttributeConverter<String, String> {

    private final String policyName;

    protected CypheraConverter(String policyName) {
        this.policyName = policyName;
    }

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null) return null;
        return CypheraHolder.get().protect(value, policyName);
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return CypheraHolder.get().access(dbValue);
    }
}
