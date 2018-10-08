package org.axonframework.cdi;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import javax.enterprise.inject.spi.Bean;

/**
 * @author Milan Savic
 */
public class MessageHandlingBeanDefinition {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final Bean<?> bean;
    private final boolean eventHandler;
    private final boolean queryHandler;
    private final boolean commandHandler;

    public MessageHandlingBeanDefinition(Bean<?> bean) {
        this.bean = bean;
        this.eventHandler = CdiUtilities.hasAnnotatedMethod(bean, EventHandler.class);
        this.queryHandler = CdiUtilities.hasAnnotatedMethod(bean, QueryHandler.class);
        this.commandHandler = CdiUtilities.hasAnnotatedMethod(bean, CommandHandler.class);
    }

    public MessageHandlingBeanDefinition(Bean<?> bean, boolean eventHandler,
            boolean queryHandler, boolean commandHandler) {
        this.bean = bean;
        this.eventHandler = eventHandler;
        this.queryHandler = queryHandler;
        this.commandHandler = commandHandler;
    }

    public static Optional<MessageHandlingBeanDefinition> inspect(Bean<?> bean) {
        boolean isEventHandler = CdiUtilities.hasAnnotatedMethod(bean, EventHandler.class);
        boolean isQueryHandler = CdiUtilities.hasAnnotatedMethod(bean, QueryHandler.class);
        boolean isCommandHandler = CdiUtilities.hasAnnotatedMethod(bean, CommandHandler.class);

        if (isEventHandler || isQueryHandler || isCommandHandler) {
            return Optional.of(new MessageHandlingBeanDefinition(bean,
                    isEventHandler, isQueryHandler, isCommandHandler));
        }

        final String className = bean.getBeanClass().getName();
        if (className.startsWith("io.axoniq")) {
            logger.debug("inspect(): {} has no relevant annotations.", className);
        }
        return Optional.empty();
    }

    public static boolean isMessageHandler(Class<?> clazz) {
        return CdiUtilities.hasAnnotatedMethod(clazz,
                EventHandler.class, EventSourcingHandler.class, QueryHandler.class, CommandHandler.class);
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
