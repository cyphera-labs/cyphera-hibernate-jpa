package io.cyphera.jpa;

import io.cyphera.Cyphera;
import jakarta.persistence.AttributeConverter;

/**
 * Base JPA AttributeConverter for Cyphera format-preserving encryption.
 *
 * Subclass this and set the policy name to create a converter for any field type:
 *
 *   @Converter
 *   public class SsnConverter extends CypheraConverter {
 *       public SsnConverter() { super("ssn"); }
 *   }
 *
 * Then on your entity:
 *
 *   @Convert(converter = SsnConverter.class)
 *   private String ssn;
 *
 * Data is protected on write (convertToDatabaseColumn) and accessed on read
 * (convertToEntityAttribute) using the embedded tag — transparent to the application.
 */
public abstract class CypheraConverter implements AttributeConverter<String, String> {

    private final String policyName;
    private final Cyphera client;

    protected CypheraConverter(String policyName) {
        this.policyName = policyName;
        this.client = CypheraLoader.getInstance();
    }

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null) return null;
        return client.protect(value, policyName);
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return client.access(dbValue);
    }
}
