package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

@Vetoed
public class DepositMoneyCommand {

    @TargetAggregateIdentifier
    @NotNull
    private final String accountId;

    @NotNull
    @Min(0)
    private final BigDecimal amount;

    public DepositMoneyCommand(String accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "DepositMoneyCommand [accountId=" + accountId + ", amount=" + amount + "]";
    }
}
