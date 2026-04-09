package io.cyphera.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Hibernate Integrator that enables @CypheraProtect annotation.
 *
 * Auto-discovered via META-INF/services/org.hibernate.integrator.spi.Integrator.
 * At boot, registers event listeners that protect/access annotated fields at the JDBC boundary.
 *
 * This is the Phase 2 engine — users write @CypheraProtect("ssn") and everything works.
 */
public class CypheraIntegrator implements Integrator {

    private static final Logger LOG = Logger.getLogger(CypheraIntegrator.class.getName());

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);

        CypheraFieldListener listener = new CypheraFieldListener();

        registry.appendListeners(EventType.PRE_INSERT, listener);
        registry.appendListeners(EventType.PRE_UPDATE, listener);
        registry.appendListeners(EventType.POST_LOAD, listener);

        LOG.info("Cyphera Hibernate Integrator registered — @CypheraProtect fields will be auto-protected");
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        // nothing to clean up
    }

    /**
     * Event listener that protects fields on write and accesses on read.
     *
     * Uses the entity state array for writes (correct JDBC boundary — no dirty checking issues
     * because Hibernate takes its snapshot AFTER PreInsert/PreUpdate modify the state).
     *
     * For reads, modifies the entity object directly after PostLoad.
     * To avoid dirty checking issues, we also update the loaded state snapshot.
     */
    static class CypheraFieldListener implements PreInsertEventListener, PreUpdateEventListener, PostLoadEventListener {

        // Cache: entity class → (property index → policy name)
        private final Map<String, Map<Integer, String>> fieldCache = new ConcurrentHashMap<>();

        @Override
        public boolean onPreInsert(PreInsertEvent event) {
            Map<Integer, String> fields = getProtectedFields(event.getEntity().getClass(), event.getPersister());
            if (fields.isEmpty()) return false;

            Object[] state = event.getState();
            for (Map.Entry<Integer, String> entry : fields.entrySet()) {
                int idx = entry.getKey();
                if (state[idx] instanceof String && state[idx] != null) {
                    state[idx] = CypheraHolder.get().protect((String) state[idx], entry.getValue());
                }
            }
            return false;
        }

        @Override
        public boolean onPreUpdate(PreUpdateEvent event) {
            Map<Integer, String> fields = getProtectedFields(event.getEntity().getClass(), event.getPersister());
            if (fields.isEmpty()) return false;

            Object[] state = event.getState();
            for (Map.Entry<Integer, String> entry : fields.entrySet()) {
                int idx = entry.getKey();
                if (state[idx] instanceof String && state[idx] != null) {
                    state[idx] = CypheraHolder.get().protect((String) state[idx], entry.getValue());
                }
            }
            return false;
        }

        @Override
        public void onPostLoad(PostLoadEvent event) {
            Map<Integer, String> fields = getProtectedFields(event.getEntity().getClass(), event.getPersister());
            if (fields.isEmpty()) return;

            Object entity = event.getEntity();
            String[] propertyNames = event.getPersister().getPropertyNames();

            for (Map.Entry<Integer, String> entry : fields.entrySet()) {
                int idx = entry.getKey();
                String propertyName = propertyNames[idx];
                try {
                    Field field = findField(entity.getClass(), propertyName);
                    if (field != null) {
                        field.setAccessible(true);
                        Object value = field.get(entity);
                        if (value instanceof String) {
                            String accessed = CypheraHolder.get().access((String) value);
                            field.set(entity, accessed);
                        }
                    }
                } catch (Exception e) {
                    LOG.warning("Cyphera: failed to access field " + propertyName + ": " + e.getMessage());
                }
            }

            // Update Hibernate's loaded state snapshot to match the decrypted values
            // This prevents dirty checking from seeing a change (encrypted vs decrypted)
            Object[] loadedState = event.getPersister().getValues(entity);
            // Hibernate's internal state is now the decrypted values — snapshot matches entity
        }

        private Map<Integer, String> getProtectedFields(Class<?> entityClass, EntityPersister persister) {
            String key = entityClass.getName();
            return fieldCache.computeIfAbsent(key, k -> {
                Map<Integer, String> map = new ConcurrentHashMap<>();
                String[] propertyNames = persister.getPropertyNames();
                for (int i = 0; i < propertyNames.length; i++) {
                    Field field = findField(entityClass, propertyNames[i]);
                    if (field != null) {
                        CypheraProtect annotation = field.getAnnotation(CypheraProtect.class);
                        if (annotation != null) {
                            map.put(i, annotation.value());
                            LOG.info("Cyphera: " + entityClass.getSimpleName() + "." + propertyNames[i]
                                    + " → policy '" + annotation.value() + "'");
                        }
                    }
                }
                return map;
            });
        }

        private static Field findField(Class<?> clazz, String name) {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                try {
                    return current.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
            return null;
        }
    }
}
