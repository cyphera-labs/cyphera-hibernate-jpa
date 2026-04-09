package io.cyphera.jpa;

import jakarta.persistence.Converter;

/**
 * JPA converter for credit card fields. Uses the "credit_card" Cyphera policy.
 *
 * Usage:
 *   @Convert(converter = CreditCardConverter.class)
 *   private String creditCard;
 */
@Converter
public class CreditCardConverter extends CypheraConverter {
    public CreditCardConverter() { super("credit_card"); }
}
