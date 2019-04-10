package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

@Vetoed
public class CreateAccountCommand {

    @TargetAggregateIdentifier
    @NotNull
    private final String accountId;

    @NotNull
    @Min(0)
    private final BigDecimal overdraftLimit;

    public CreateAccountCommand(String accountId, BigDecimal overdraftLimit) {
        this.accountId = accountId;
        this.overdraftLimit = overdraftLimit;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public String toString() {
        return "CreateAccountCommand [accountId=" + accountId + ", overdraftLimit=" + overdraftLimit + "]";
    }
}
