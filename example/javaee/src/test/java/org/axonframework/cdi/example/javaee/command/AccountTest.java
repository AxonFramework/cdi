package org.axonframework.cdi.example.javaee.command;

import java.math.BigDecimal;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.Test;

public class AccountTest {

    private final AggregateTestFixture<Account> account
            = new AggregateTestFixture<>(Account.class);

    @Test
    public void testAccountCreate() {
        account.givenNoPriorActivity()
                .when(new CreateAccountCommand("4711", BigDecimal.valueOf(100.00)))
                .expectEvents(new AccountCreatedEvent("4711", BigDecimal.valueOf(100.00)));
    }

    @Test
    public void testWithdrawMoney() {
        account.given(new AccountCreatedEvent("4711", BigDecimal.valueOf(100.00)))
                .when(new WithdrawMoneyCommand("4711", BigDecimal.valueOf(50.00)))
                .expectEvents(new MoneyWithdrawnEvent("4711", BigDecimal.valueOf(50.00)));
    }

    @Test
    public void testWithdrawMoneyExceedingOverdraftLimit() {
        account.given(new AccountCreatedEvent("4711", BigDecimal.valueOf(100.00)))
                .when(new WithdrawMoneyCommand("4711", BigDecimal.valueOf(150.00)))
                .expectException(WithdrawalDeniedException.class);
    }

    @Test
    public void testDepositMoney() {
        account.given(new AccountCreatedEvent("4711", BigDecimal.valueOf(100.00)))
                .when(new DepositMoneyCommand("4711", BigDecimal.valueOf(50.00)))
                .expectEvents(new MoneyDepositedEvent("4711", BigDecimal.valueOf(50.00)));
    }

    @Test
    public void testWithrawMoneyFromDepositedMoney() {
        account.given(new AccountCreatedEvent("4711", BigDecimal.valueOf(100.00)))
                .andGiven(new MoneyDepositedEvent("4711", BigDecimal.valueOf(50.00)))
                .when(new WithdrawMoneyCommand("4711", BigDecimal.valueOf(150.00)))
                .expectEvents(new MoneyWithdrawnEvent("4711", BigDecimal.valueOf(150.00)));
    }
}
