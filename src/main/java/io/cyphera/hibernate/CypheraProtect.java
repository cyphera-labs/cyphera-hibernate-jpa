package io.cyphera.hibernate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field for automatic Cyphera format-preserving encryption.
 *
 * Data is protected on write and accessed on read — transparent to your code.
 * The CypheraIntegrator discovers this annotation at boot and registers
 * CypheraType for each annotated field. No converter classes, no boilerplate.
 *
 * Usage:
 *
 *   @Entity
 *   public class Customer {
 *       @CypheraProtect("ssn")
 *       private String ssn;
 *
 *       @CypheraProtect("credit_card")
 *       private String creditCard;
 *   }
 *
 * The value is the Cyphera policy name as defined in cyphera.json.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CypheraProtect {
    /**
     * The Cyphera policy name (e.g. "ssn", "credit_card", "phone").
     */
    String value();
}
