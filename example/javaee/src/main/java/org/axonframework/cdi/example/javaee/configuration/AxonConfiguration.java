package org.axonframework.cdi.example.javaee.configuration;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.NoTransactionManager;
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

	@PersistenceContext(unitName = "account")
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

	@Produces
	public TransactionManager transactionManager() {
		// TODO See if Axon can work with a managed JTA transaction manager.
		return NoTransactionManager.INSTANCE;
	}

	/**
	 * Produces container transaction aware JPA storage engine.
	 *
	 * @return Event storage engine.
	 */
	@Produces
	public EventStorageEngine eventStorageEngine() {
		return new JpaEventStorageEngine(getEntityManagerProvider(), transactionManager());
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
