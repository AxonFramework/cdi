package org.axonframework.extension.example.common.core.api;

import lombok.Value;

@Value
public class MoneyWithdrawnEvent {
  String accountId;
  Integer amount;
}
