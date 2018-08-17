package org.axonframework.extension.example.common.query;

import javax.inject.Named;

import org.axonframework.eventhandling.EventHandler;

import lombok.extern.slf4j.Slf4j;

@Named
@Slf4j
public class GenericEventListener {

  @EventHandler
  public void on(Object event) {
    log.info("Event received {}", event);
  }

}
