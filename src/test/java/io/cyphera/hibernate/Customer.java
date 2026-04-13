package io.cyphera.hibernate;

import jakarta.persistence.*;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Type(value = CypheraType.class, parameters = @Parameter(name = "policy", value = "ssn"))
    private String ssn;

    private String email;

    public Customer() {}

    public Customer(String name, String ssn, String email) {
        this.name = name;
        this.ssn = ssn;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSsn() { return ssn; }
    public String getEmail() { return email; }
}
