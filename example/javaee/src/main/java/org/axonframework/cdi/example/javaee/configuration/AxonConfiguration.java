package org.axonframework.cdi.example.javaee.configuration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.axonframework.cdi.transaction.ContainerTransactionManager;
import org.axonframework.cdi.transaction.LoggingTransactionManager;
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
public class AxonConfiguration {

    /**
     * WildFly JNDI address for JTA UserTransaction.
     */
    // TODO This should be abstracted into the module. Most users would not know
    // this.
    private static final String JBOSS_USER_TRANSACTION = "java:jboss/UserTransaction";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Produces the entity manager provider.
     *
     * @return entity manager provider.
     */
    @Produces
    public EntityManagerProvider getEntityManagerProvider() {
        return new SimpleEntityManagerProvider(entityManager);
    }

    /**
     * Produces the transaction manager.
     *
     * @return transaction manager.
     */
    // @Produces
    public TransactionManager containerTransactionManager() {
        return new ContainerTransactionManager(entityManager, JBOSS_USER_TRANSACTION);
    }

    @Produces
    public TransactionManager transactionManager() {
        return LoggingTransactionManager.INSTANCE;
    }

    /**
     * Produces container transaction aware JPA storage engine.
     *
     * @return Event storage engine.
     */
    @Produces
    public EventStorageEngine eventStorageEngine() {
        return new JpaEventStorageEngine(getEntityManagerProvider(), transactionManager());
        // return new
        // TransactionAwareJPAEventStorageEngine(getEntityManagerProvider(),
        // containerTransactionManager());
    }

    /**
     * Produces JPA token store.
     *
     * @return token store.
     */
    @Produces
    public TokenStore tokenStore() {
        return new JpaTokenStore(getEntityManagerProvider(), serializer());
    }

    /**
     * Produces Jackson serializer.
     *
     * @return serializer.
     */
    @Produces
    public Serializer serializer() {
        return new JacksonSerializer();
    }
}
