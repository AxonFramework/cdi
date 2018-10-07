package org.axonframework.cdi.config;

import org.axonframework.cdi.MessageHandlingBeanDefinition;
import org.axonframework.config.Component;
import org.axonframework.config.Configurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.BeanManager;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class BeanConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final Map<Class<?>, MessageHandlingBeanDefinition> messageHandlers = new HashMap<>();

    public void addMessageHandler(Class<?> beanClass, MessageHandlingBeanDefinition bean) {
        messageHandlers.put(beanClass, bean);
    }

    public <T> boolean isMessageHandler(Class<T> clazz) {
        return messageHandlers.containsKey(clazz);
    }

    public void registerMessageHandlers(BeanManager beanManager, Configurer configurer,
                                         EventHandlingConfiguration eventHandlingConfiguration) {
        for (MessageHandlingBeanDefinition messageHandler : messageHandlers.values()) {
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
