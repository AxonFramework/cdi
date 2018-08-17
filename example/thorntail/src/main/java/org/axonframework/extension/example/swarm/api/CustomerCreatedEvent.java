package org.axonframework.extension.example.swarm.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CustomerCreatedEvent {
  private final String customerId;
  private final String fullName;
  private final Integer age;
}
