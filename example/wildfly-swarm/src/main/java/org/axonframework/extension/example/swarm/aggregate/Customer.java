package org.axonframework.extension.example.swarm.aggregate;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.cdi.stereotype.Aggregate;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.extension.example.swarm.api.CreateCustomerCommand;
import org.axonframework.extension.example.swarm.api.CustomerCreatedEvent;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@Slf4j
@Aggregate
@Getter
public class Customer {

  @AggregateIdentifier
  private String customerId;

  private String fullName;
  private int age;

  public Customer() {
  }

  @CommandHandler
  public Customer(CreateCustomerCommand cmd) {
    log.info("Received CreateCustomerCommand: {}", cmd);
    apply(new CustomerCreatedEvent(cmd.getCustomerId(), cmd.getFullName(), cmd.getAge()));
  }

  @EventSourcingHandler
  public void on(CustomerCreatedEvent event) {
    log.info("Caught CustomerCreatedEvent: {}", event);
    this.customerId = event.getCustomerId();
    this.fullName = event.getFullName();
    this.age = event.getAge();
  }
}
