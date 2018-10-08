package org.axonframework.cdi;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;

import java.util.Optional;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

/**
 * @author Milan Savic
 */
class MessageHandlingBeanDefinition {

    private final Bean<?> bean;
    private final boolean eventHandler;
    private final boolean queryHandler;
    private final boolean commandHandler;

    MessageHandlingBeanDefinition(Bean<?> bean, boolean eventHandler,
            boolean queryHandler, boolean commandHandler) {
        this.bean = bean;
        this.eventHandler = eventHandler;
        this.queryHandler = queryHandler;
        this.commandHandler = commandHandler;
    }

    static Optional<MessageHandlingBeanDefinition> inspect(Bean<?> bean, Annotated annotated) {
        if (!(annotated instanceof AnnotatedType)) {
            return Optional.empty();
        }
        AnnotatedType at = (AnnotatedType) annotated;
        boolean isEventHandler = CdiUtilities.hasAnnotatedMethod(at, EventHandler.class);
        boolean isQueryHandler = CdiUtilities.hasAnnotatedMethod(at, QueryHandler.class);
        boolean isCommandHandler = CdiUtilities.hasAnnotatedMethod(at, CommandHandler.class);

        if (isEventHandler || isQueryHandler || isCommandHandler) {
            return Optional.of(new MessageHandlingBeanDefinition(bean,
                    isEventHandler, isQueryHandler, isCommandHandler));
        }

        return Optional.empty();
    }

    public Bean<?> getBean() {
        return bean;
    }

    public boolean isEventHandler() {
        return eventHandler;
    }

    public boolean isQueryHandler() {
        return queryHandler;
    }

    public boolean isCommandHandler() {
        return commandHandler;
    }

    @Override
    public String toString() {
        return "MessageHandlingBeanDefinition with bean=" + bean
                + ", eventHandler=" + eventHandler + ", queryHandler="
                + queryHandler + ", commandHandler=" + commandHandler;
    }
}
