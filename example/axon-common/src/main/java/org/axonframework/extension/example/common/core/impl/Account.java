package org.axonframework.extension.example.common.core.impl;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

import org.axonframework.cdi.stereotype.Aggregate;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventsourcing.EventSourcingHandler;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extension.example.common.core.api.AccountCreatedEvent;
import org.axonframework.extension.example.common.core.api.CreateAccountCommand;

@Slf4j
@Aggregate
@NoArgsConstructor
public class Account {

  @AggregateIdentifier
  private String accountId;

  @CommandHandler
  public Account(final CreateAccountCommand command) {
    log.info("Create account command handled.");
    apply(new AccountCreatedEvent(command.getAccountId(), command.getOverdraftLimit()));
  }

  @EventSourcingHandler
  public void on(AccountCreatedEvent event) {
    log.info("Account created event received.");
    this.accountId = event.getAccountId();
  }
}
