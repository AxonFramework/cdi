package org.axonframework.cdi.config;

import org.axonframework.cdi.MessageHandlingBeanDefinition;
import org.axonframework.config.Component;
import org.axonframework.config.Configurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.BeanManager;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class BeanConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final Set<Class<?>> handlerClasses = new HashSet<>();
    private final List<MessageHandlingBeanDefinition> messageHandlers = new ArrayList<>();

    public void addMessageHandlerClass(Class<?> beanClass) {
        handlerClasses.add(beanClass);
    }

    public void addMessageHandler(MessageHandlingBeanDefinition bean) {
        messageHandlers.add(bean);
    }

    public <T> boolean isMessageHandler(Class<T> clazz) {
        return handlerClasses.contains(clazz);
    }

    public void registerMessageHandlers(BeanManager beanManager, Configurer configurer,
                                         EventHandlingConfiguration eventHandlingConfiguration) {
        for (MessageHandlingBeanDefinition messageHandler : messageHandlers) {
            Component<Object> component = new Component<>(() -> null, "messageHandler",
                    c -> messageHandler.getBean().create(beanManager.createCreationalContext(null)));

            if (messageHandler.isEventHandler()) {
                logger.info("Registering event handler: {}.",
                        messageHandler.getBean().getBeanClass().getSimpleName());
                eventHandlingConfiguration.registerEventHandler(c -> component.get());
            }

            if (messageHandler.isCommandHandler()) {
                logger.info("Registering command handler: {}.",
                        messageHandler.getBean().getBeanClass().getSimpleName());
                configurer.registerCommandHandler(c -> component.get());
            }

            if (messageHandler.isQueryHandler()) {
                logger.info("Registering query handler: {}.",
                        messageHandler.getBean().getBeanClass().getSimpleName());
                configurer.registerQueryHandler(c -> component.get());
            }
        }
    }
}
