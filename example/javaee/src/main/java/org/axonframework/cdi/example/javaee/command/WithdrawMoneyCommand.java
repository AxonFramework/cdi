package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;
import javax.validation.constraints.Min;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

@Vetoed
public class WithdrawMoneyCommand {

    @TargetAggregateIdentifier
    private final String accountId;

    @Min(0)
    private final BigDecimal amount;

    public WithdrawMoneyCommand(String accountId, BigDecimal amount) {
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
        return "WithdrawMoneyCommand [accountId=" + accountId + ", amount=" + amount + "]";
    }
}
