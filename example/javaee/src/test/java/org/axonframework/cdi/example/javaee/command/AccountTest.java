package org.axonframework.cdi.example.javaee.command;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.Test;

public class AccountTest {

    private final AggregateTestFixture<Account> account
            = new AggregateTestFixture<>(Account.class);

    @Test
    public void testAccountCreate() {
        account.givenNoPriorActivity()
                .when(new CreateAccountCommand("4711", 100.00))
                .expectEvents(new AccountCreatedEvent("4711", 100.00));
    }
}
