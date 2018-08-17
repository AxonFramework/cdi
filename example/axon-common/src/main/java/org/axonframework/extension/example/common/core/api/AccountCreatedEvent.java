package org.axonframework.extension.example.common.core.api;

import lombok.Value;

@Value
public class AccountCreatedEvent {
  String accountId;
  Integer overdraftLimit;
}
