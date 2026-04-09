package io.cyphera.hibernate;

import io.cyphera.Cyphera;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

/**
 * Hibernate UserType that provides transparent format-preserving encryption.
 *
 * On write: protects the value using the configured Cyphera policy.
 * On read: accesses (decrypts) the value using the embedded tag.
 * Dirty checking works correctly — Hibernate snapshots the plaintext Java value.
 *
 * Phase 1 usage (explicit @Type):
 *   @Type(value = CypheraType.class, parameters = @Parameter(name = "policy", value = "ssn"))
 *   private String ssn;
 *
 * Phase 2 usage (coming — clean @Cyphera annotation):
 *   @Cyphera(policy = "ssn")
 *   private String ssn;
 */
public class CypheraType implements UserType<String>, ParameterizedType {

    private String policyName;

    @Override
    public void setParameterValues(Properties parameters) {
        this.policyName = parameters.getProperty("policy");
        if (policyName == null || policyName.isEmpty()) {
            throw new IllegalArgumentException("CypheraType requires 'policy' parameter");
        }
    }

    @Override
    public String nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        String dbValue = rs.getString(position);
        if (dbValue == null) return null;
        return CypheraHolder.get().access(dbValue);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
            return;
        }
        st.setString(index, CypheraHolder.get().protect(value, policyName));
    }

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(String x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public String deepCopy(String value) {
        return value; // String is immutable
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String value) {
        return value;
    }

    @Override
    public String assemble(Serializable cached, Object owner) {
        return (String) cached;
    }
}
