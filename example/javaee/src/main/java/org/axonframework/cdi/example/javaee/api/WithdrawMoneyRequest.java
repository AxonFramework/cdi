package org.axonframework.cdi.example.javaee.api;

import java.math.BigDecimal;

import javax.enterprise.inject.Vetoed;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Vetoed
public class WithdrawMoneyRequest {

    @Min(0)
    @NotNull
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "WithdrawMoneyRequest [amount=" + amount + "]";
    }
}
