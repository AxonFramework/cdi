package org.axonframework.extension.example.wildfly;

import lombok.extern.slf4j.Slf4j;
import org.apache.deltaspike.cdise.api.CdiContainer;
import org.apache.deltaspike.cdise.api.CdiContainerLoader;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.interceptors.EventLoggingInterceptor;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.extension.example.common.core.api.CreateAccountCommand;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

/**
 * Main entry class.
 */
@Slf4j
public class CdiApplication {

  @Inject
  private EventBus eventBus;

  @Inject
  private CommandGateway commandGateway;

  /**
   * Produces container transaction aware JPA storage engine.
   *
   * @return Event storage engine.
   */
  @Produces
  public EventStorageEngine eventStorageEngine() {
    return new InMemoryEventStorageEngine();
  }


  /**
   * Main method interacting with the application.
   */
  public void run() {
    eventBus.registerDispatchInterceptor(new EventLoggingInterceptor());
    commandGateway.send(new CreateAccountCommand("4711", 1000));
  }

  /**
   * Initializer for CDI.
   * @param args params.
   */
  public static void main(final String[] args) {
    final CdiContainer container = CdiContainerLoader.getCdiContainer();
    try {
      container.boot();
      final CdiApplication application = CDI.current().select(CdiApplication.class).get();
      application.run();
    } catch (Exception e) {
      log.error("Error in example", e);
    } finally {
      container.shutdown();
      log.info("Shutting down...");
    }
  }

}
