package org.axonframework.cdi.example.javase.saga;

import org.axonframework.cdi.example.javase.command.AccountCreatedEvent;
import org.axonframework.cdi.stereotype.Saga;
import org.axonframework.eventhandling.saga.SagaEventHandler;

/**
 * @author Milan Savic
 */
@Saga(sagaStore = "mySaga1Store")
public class MySaga {

    @SagaEventHandler(associationProperty = "accountId")
    public void on(AccountCreatedEvent evt) {

    }
}
