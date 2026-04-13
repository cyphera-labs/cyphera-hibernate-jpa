package io.cyphera.hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class CypheraHibernateTest {

    static EntityManagerFactory emf;

    @BeforeAll
    static void setup() {
        // Point CypheraHolder to test policy file
        String policyPath = CypheraHibernateTest.class.getClassLoader().getResource("cyphera.json").getPath();
        io.cyphera.Cyphera cyphera = io.cyphera.Cyphera.fromFile(policyPath);
        CypheraHolder.set(cyphera);

        Configuration cfg = new Configuration();
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.connection.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");
        cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        cfg.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        cfg.setProperty("hibernate.show_sql", "true");
        cfg.addAnnotatedClass(Customer.class);

        emf = cfg.buildSessionFactory();
    }

    @AfterAll
    static void teardown() {
        if (emf != null) emf.close();
    }

    @Test
    void protectOnWrite_accessOnRead() {
        // Write
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Customer c = new Customer("Alice Johnson", "123-45-6789", "alice@example.com");
        em.persist(c);
        em.getTransaction().commit();
        Long id = c.getId();
        em.close();

        // Read back — should be plaintext (CypheraType decrypts transparently)
        em = emf.createEntityManager();
        Customer loaded = em.find(Customer.class, id);
        assertEquals("Alice Johnson", loaded.getName());
        assertEquals("123-45-6789", loaded.getSsn(), "SSN should be decrypted on read");
        assertEquals("alice@example.com", loaded.getEmail());
        em.close();
    }

    @Test
    void databaseContainsEncryptedValue() throws Exception {
        // Write via Hibernate
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Customer c = new Customer("Bob Smith", "987-65-4321", "bob@example.com");
        em.persist(c);
        em.getTransaction().commit();
        Long id = c.getId();
        em.close();

        // Read raw from DB via JDBC — should be encrypted
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT ssn FROM customers WHERE id = " + id);
        assertTrue(rs.next());
        String rawSsn = rs.getString("ssn");
        conn.close();

        assertNotEquals("987-65-4321", rawSsn, "Raw DB value should NOT be plaintext");
        assertTrue(rawSsn.startsWith("T01"), "Raw DB value should have tag T01, got: " + rawSsn);
        assertTrue(rawSsn.contains("-"), "Dashes should be preserved in encrypted value");
    }

    @Test
    void roundtripMultipleRecords() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(new Customer("Carol Davis", "555-12-3456", "carol@example.com"));
        em.persist(new Customer("Dan Park", "111-22-3333", "dan@example.com"));
        em.persist(new Customer("Eve Liu", "444-55-6666", "eve@example.com"));
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        var customers = em.createQuery("SELECT c FROM Customer c WHERE c.name LIKE 'Carol%'", Customer.class)
            .getResultList();
        assertEquals(1, customers.size());
        assertEquals("555-12-3456", customers.get(0).getSsn());
        em.close();
    }

    @Test
    void nullSsnHandled() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Customer c = new Customer("No SSN Person", null, "nobody@example.com");
        em.persist(c);
        em.getTransaction().commit();
        Long id = c.getId();
        em.close();

        em = emf.createEntityManager();
        Customer loaded = em.find(Customer.class, id);
        assertNull(loaded.getSsn(), "null SSN should stay null");
        em.close();
    }
}
