package io.cyphera.hibernate;

import io.cyphera.Cyphera;

import java.util.logging.Logger;

/**
 * Static holder for the Cyphera SDK instance.
 *
 * Hibernate instantiates UserType classes directly — they cannot use Spring DI.
 * This holder bridges the gap.
 *
 * Initialization order:
 * 1. Spring Boot auto-config calls set() at startup (if Spring is present)
 * 2. Plain Hibernate: user calls set() at bootstrap
 * 3. If neither: get() falls back to Cyphera.load() (auto-discover)
 *
 * Idempotent: set() only takes effect once. No surprise reloads.
 */
public final class CypheraHolder {

    private static final Logger LOG = Logger.getLogger(CypheraHolder.class.getName());
    private static volatile Cyphera instance;

    private CypheraHolder() {}

    /**
     * Set the Cyphera instance. Call once at bootstrap.
     * Idempotent — second call is a no-op.
     */
    public static synchronized void set(Cyphera cyphera) {
        if (instance == null) {
            instance = cyphera;
            LOG.info("CypheraHolder initialized (explicit)");
        }
    }

    /**
     * Get the Cyphera instance.
     * Auto-discovers via Cyphera.load() if not explicitly set.
     * Fails fast with a clear error if no policy file found.
     */
    public static Cyphera get() {
        if (instance == null) {
            synchronized (CypheraHolder.class) {
                if (instance == null) {
                    LOG.info("CypheraHolder not explicitly set — auto-discovering policy file");
                    instance = Cyphera.load();
                    LOG.info("CypheraHolder initialized (auto-discover)");
                }
            }
        }
        return instance;
    }
}
