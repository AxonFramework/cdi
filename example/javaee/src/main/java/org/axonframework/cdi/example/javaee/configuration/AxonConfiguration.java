package org.axonframework.cdi.example.javaee.configuration;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.axonframework.cdi.transaction.JtaTransactionManager;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.jpa.JpaTokenStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;

@ApplicationScoped
public class AxonConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    @PersistenceContext
    private EntityManager em;

    /**
     * Produces the entity manager.
     *
     * @return entity manager.
     * @throws NamingException
     */
    @Produces
    @ApplicationScoped
    public EntityManager entityManager() throws NamingException {
        return em;
    }

    /**
     * Produces the entity manager provider.
     *
     * @return entity manager provider.
     */
    @Produces
    @ApplicationScoped
    public EntityManagerProvider entityManagerProvider(
            EntityManager entityManager) {
        return new SimpleEntityManagerProvider(entityManager);
    }

    @Produces
    @ApplicationScoped
    public TransactionManager transactionManager() {
        return new JtaTransactionManager();
        // return NoTransactionManager.INSTANCE;
    }

    /**
     * Produces container transaction aware JPA storage engine.
     *
     * @return Event storage engine.
     */
    @Produces
    @ApplicationScoped
    public EventStorageEngine eventStorageEngine(
            EntityManagerProvider entityManagerProvider,
            TransactionManager transactionManager) {
        return new JpaEventStorageEngine(entityManagerProvider, transactionManager);
    }

    /**
     * Produces JPA token store.
     *
     * @return token store.
     */
    @Produces
    @ApplicationScoped
    public TokenStore tokenStore(EntityManagerProvider entityManagerProvider,
            Serializer serializer) {
        return new JpaTokenStore(entityManagerProvider, serializer);
    }

    /**
     * Produces Jackson serializer.
     *
     * @return serializer.
     */
    @Produces
    @ApplicationScoped
    public Serializer serializer() {
        return new JacksonSerializer();
    }
}
