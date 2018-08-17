package org.axonframework.extension.example.common.query;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.extension.example.common.core.api.AccountCreatedEvent;

import javax.inject.Named;

@Named
@Slf4j
public class AccountListener {

  @EventHandler
  public void on(AccountCreatedEvent event) {
    log.info("Account created event received: {}", event);
  }

}
