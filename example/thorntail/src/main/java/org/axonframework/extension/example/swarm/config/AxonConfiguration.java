package org.axonframework.extension.example.swarm.config;

import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.ContainerTransactionManager;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@ApplicationScoped
public class AxonConfiguration {

  private static final String userTransaction = "java:comp/UserTransaction";

  @PersistenceContext(name = "MyPU")
  private EntityManager em;

  @Produces
  public TransactionManager transactionManager() {
    return new ContainerTransactionManager(em, userTransaction);
  }

  @Produces
  public EntityManagerProvider entityManagerProvider() {
    return new SimpleEntityManagerProvider(em);
  }

  @Produces
  public EventStorageEngine eventStorageEngine(final EntityManagerProvider emp, final TransactionManager transactionManager) {
    return new JpaEventStorageEngine(emp, transactionManager);
  }
}
