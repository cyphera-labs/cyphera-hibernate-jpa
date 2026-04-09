package io.cyphera.jpa;

import jakarta.persistence.Converter;

/**
 * JPA converter for SSN fields. Uses the "ssn" Cyphera policy.
 *
 * Usage:
 *   @Convert(converter = SsnConverter.class)
 *   private String ssn;
 */
@Converter
public class SsnConverter extends CypheraConverter {
    public SsnConverter() { super("ssn"); }
}
