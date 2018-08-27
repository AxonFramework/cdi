package org.axonframework.cdi.example.javase.configuration;

import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.axonframework.cdi.example.javase.saga.MySaga;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.saga.repository.CachingSagaStore;
import org.axonframework.eventhandling.saga.repository.SagaStore;
import org.axonframework.eventhandling.saga.repository.inmemory.InMemorySagaStore;
import org.axonframework.eventhandling.saga.repository.jdbc.JdbcSagaStore;
import org.axonframework.eventhandling.saga.repository.jpa.JpaSagaStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;

@ApplicationScoped
public class AxonConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * In a Java SE environment, we are using an in-memory store.
     *
     * @return Event storage engine.
     */
    @Produces
    @ApplicationScoped
    public EventStorageEngine eventStorageEngine() {
        return new InMemoryEventStorageEngine();
    }

    @Produces
    @ApplicationScoped
    public SagaStore mySaga1Store() {
        InMemorySagaStore inMemorySagaStore = new InMemorySagaStore();
        System.out.println("mySaga1Store" + inMemorySagaStore);
        return inMemorySagaStore;
    }

    @Produces
    @ApplicationScoped
    public SagaStore defaultSagaStore() {
        JdbcSagaStore jdbcSagaStore = new JdbcSagaStore(null);
        System.out.println("jdbcSagaStore" + jdbcSagaStore);
        return jdbcSagaStore;
    }
}
