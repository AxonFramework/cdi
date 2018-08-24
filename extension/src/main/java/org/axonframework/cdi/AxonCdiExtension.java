package org.axonframework.cdi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Named;

import org.axonframework.cdi.stereotype.Aggregate;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.axonframework.config.ModuleConfiguration;
import org.axonframework.eventhandling.ErrorHandler;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.EventUpcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main CDI extension class responsible for collecting CDI beans and setting up
 * Axon configuration.
 */
// TODO: Possibly missing scanning/configuration/registration/injection:
// * Repository<Aggregate.Type>
// * EventScheduler
// * @SagaEventHandler
// * EventStore
// * SagaStore
// * Snapshotter
// * SnapshotTriggerDefinition
public class AxonCdiExtension implements Extension {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private Configuration configuration;

    private final List<Class<?>> aggregates = new ArrayList<>();
    private final List<Bean<?>> eventHandlers = new ArrayList<>();

    private Producer<EventStorageEngine> eventStorageEngineProducer;
    private Producer<Serializer> serializerProducer;
    private Producer<Serializer> eventSerializerProducer;
    private Producer<EventBus> eventBusProducer;
    private Producer<CommandBus> commandBusProducer;
    private Producer<Configurer> configurerProducer;
    private Producer<TransactionManager> transactionManagerProducer;
    private Producer<EntityManagerProvider> entityManagerProviderProducer;
    private Producer<TokenStore> tokenStoreProducer;
    private Producer<ListenerInvocationErrorHandler> listenerInvocationErrorHandlerProducer;
    private Producer<ErrorHandler> errorHandlerProducer;
    private List<Producer<CorrelationDataProvider>> correlationDataProviderProducers = new ArrayList<>();
    private Producer<QueryBus> queryBusProducer;
    private List<Producer<ModuleConfiguration>> moduleConfigurationProducers = new ArrayList<>();
    private List<Producer<EventUpcaster>> eventUpcasterProducers = new ArrayList<>();

    // Antoine: Many of the beans and producers I am processing may use
    // container resources such as entity managers, etc. I believe this means
    // I should be handling them as late as possible to avoid initialization
    // timing issues. Right now things are processed as they make
    // "semantic" sense. Do you think this could be improved to do the same
    // processing later?
    /**
     * Scans all annotated types with the {@link Aggregate} annotation and
     * collects them for registration.
     *
     * @param processAnnotatedType annotated type processing event.
     */
    <T> void processAggregate(@Observes @WithAnnotations({Aggregate.class})
            final ProcessAnnotatedType<T> processAnnotatedType) {
        final Class<?> clazz = processAnnotatedType.getAnnotatedType().getJavaClass();

        logger.debug("Found Aggregate: {}.", clazz);

        aggregates.add(clazz);
    }

    // Antoine: While processing the producers, I can detect configuration
    // errors from an Axon standpoint. These errors should result in the
    // deployment failing. Should I wait to throw these validation errors until
    // later or should I do it right now during annotation scanning? Is there a
    // specific type of exception that's better to throw or will any runtime
    // exception do?
    /**
     * Scans for an event storage engine producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processEventStorageEngineProducer(
            @Observes final ProcessProducer<T, EventStorageEngine> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for EventStorageEngine found: {}.",
                processProducer.getProducer());

        this.eventStorageEngineProducer = processProducer.getProducer();
    }

    /**
     * Scans for a configurer producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processConfigurerProducer(
            @Observes final ProcessProducer<T, Configurer> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for Configurer found: {}.", processProducer.getProducer());

        this.configurerProducer = processProducer.getProducer();
    }

    /**
     * Scans for a transaction manager producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processTransactionManagerProducer(
            @Observes final ProcessProducer<T, TransactionManager> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for TransactionManager found: {}.",
                processProducer.getProducer());

        this.transactionManagerProducer = processProducer.getProducer();
    }

    /**
     * Scans for a serializer producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processSerializerProducer(
            @Observes final ProcessProducer<T, Serializer> processProducer,
            final BeanManager beanManager) {
        logger.debug("Producer for Serializer found: {}.", processProducer.getProducer());
        AnnotatedMember<T> annotatedMember = processProducer.getAnnotatedMember();
        Named qualifier = annotatedMember.getAnnotation(Named.class);
        if (qualifier != null) {
            String qualifierValue = qualifier.value();
            String serializerName = "".equals(qualifierValue)
                    ? annotatedMember.getJavaMember().getName()
                    : qualifierValue;
            switch (serializerName) {
                case "eventSerializer":
                    eventSerializerProducer = processProducer.getProducer();
                    break;
                case "":
                    this.serializerProducer = processProducer.getProducer();
                    break;
                default:
                    logger.warn("Unknown serializer configured: " + serializerName);
            }
        } else {
            this.serializerProducer = processProducer.getProducer();
        }
    }

    /**
     * Scans for an event bus producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processEventBusProducer(
            @Observes final ProcessProducer<T, EventBus> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for EventBus found: {}.", processProducer.getProducer());

        this.eventBusProducer = processProducer.getProducer();
    }

    /**
     * Scans for a command bus producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processCommandBusProducer(
            @Observes final ProcessProducer<T, CommandBus> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for CommandBus found: {}.",
                processProducer.getProducer());

        this.commandBusProducer = processProducer.getProducer();
    }

    /**
     * Scans for an entity manager provider producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processEntityManagerProviderProducer(
            @Observes final ProcessProducer<T, EntityManagerProvider> processProducer,
            final BeanManager beanManager) {
        // TODO Investigate if there is a way to look up the entity manager
        // from the environment. There likely isn't.
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for EntityManagerProvider found: {}.",
                processProducer.getProducer());

        this.entityManagerProviderProducer = processProducer.getProducer();
    }

    /**
     * Scans for a token store producer.
     *
     * @param processProducer process producer event.
     * @param beanManager bean manager.
     */
    <T> void processTokenStoreProducer(
            @Observes final ProcessProducer<T, TokenStore> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for TokenStore: {}.", processProducer.getProducer());

        this.tokenStoreProducer = processProducer.getProducer();
    }

    <T> void processErrorHandlerProducer(
            @Observes final ProcessProducer<T, ErrorHandler> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for ErrorHandler: {}.", processProducer.getProducer());

        this.errorHandlerProducer = processProducer.getProducer();
    }

    <T> void processListenerInvocationErrorHandlerProducer(
            @Observes final ProcessProducer<T, ListenerInvocationErrorHandler> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for ListenerInvocationErrorHandler: {}.", processProducer.getProducer());

        this.listenerInvocationErrorHandlerProducer = processProducer.getProducer();
    }

    <T> void processCorrelationDataProviderProducer(
            @Observes final ProcessProducer<T, CorrelationDataProvider> processProducer,
            final BeanManager beanManager) {
        logger.debug("Producer for CorrelationDataProvider: {}.", processProducer.getProducer());

        this.correlationDataProviderProducers.add(processProducer.getProducer());
    }

    <T> void processQueryBusProducer(
            @Observes final ProcessProducer<T, QueryBus> processProducer,
            final BeanManager beanManager) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for QueryBus: {}.", processProducer.getProducer());

        this.queryBusProducer = processProducer.getProducer();
    }

    <T> void processModuleConfigurationProducer(
            @Observes final ProcessProducer<T, ModuleConfiguration> processProducer,
            final BeanManager beanManager) {
        logger.debug("Producer for ModuleConfiguration: {}.", processProducer.getProducer());

        this.moduleConfigurationProducers.add(processProducer.getProducer());
    }

    <T> void processEventUpcasterProducer(
            @Observes final ProcessProducer<T, EventUpcaster> processProducer,
            final BeanManager beanManager) {
        logger.debug("Producer for EventUpcaster: {}.", processProducer.getProducer());

        this.eventUpcasterProducers.add(processProducer.getProducer());
    }

    /**
     * Scans all beans and collects beans with {@link EventHandler} annotated
     * methods.
     *
     * @param processBean bean processing event.
     */
    <T> void processBean(@Observes final ProcessBean<T> processBean) {
        final Bean<?> bean = processBean.getBean();

        if (CdiUtilities.hasAnnotatedMethod(bean, EventHandler.class)) {
            logger.debug("Found event handler {}.", bean.getBeanClass().getSimpleName());

            eventHandlers.add(bean);
        }
    }

    /**
     * Registration of Axon components in CDI registry.
     *
     * @param afterBeanDiscovery after bean discovery event.
     * @param beanManager bean manager.
     */
    void afterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery,
            final BeanManager beanManager) {
        logger.info("Starting Axon Framework configuration.");

        // Configurer registration.
        final Configurer configurer;

        if (this.configurerProducer != null) {
            // Antoine: Is createCreationalContext(null) is correct here?
            // If not, what should I do instead? Again, many of these things
            // may be indirectly referencing container resources.
            configurer = this.configurerProducer.produce(
                    beanManager.createCreationalContext(null));
        } else {
            configurer = DefaultConfigurer.defaultConfiguration();
        }

        // Entity manager provider registration.
        if (this.entityManagerProviderProducer != null) {
            final EntityManagerProvider entityManagerProvider
                    = this.entityManagerProviderProducer.produce(
                            beanManager.createCreationalContext(null));

            logger.info("Registering entity manager provider {}.",
                    entityManagerProvider.getClass().getSimpleName());

            configurer.registerComponent(EntityManagerProvider.class,
                    c -> entityManagerProvider);
        }

        // Serializer registration.
        if (this.serializerProducer != null) {
            final Serializer serializer = this.serializerProducer.produce(
                    beanManager.createCreationalContext(null));

            logger.info("Registering serializer {}.",
                    serializer.getClass().getSimpleName());

            configurer.configureSerializer(c -> serializer);
        }

        // Event Serializer registration.
        if (this.eventSerializerProducer != null) {
            final Serializer serializer = this.eventSerializerProducer.produce(
                    beanManager.createCreationalContext(null));

            logger.info("Registering event serializer {}.",
                        serializer.getClass().getSimpleName());

            configurer.configureEventSerializer(c -> serializer);
        }

        // Transaction manager registration.
        if (this.transactionManagerProducer != null) {
            final TransactionManager transactionManager
                    = this.transactionManagerProducer.produce(
                            beanManager.createCreationalContext(null));

            logger.info("Registering transaction manager {}.",
                    transactionManager.getClass().getSimpleName());

            configurer.configureTransactionManager(c -> transactionManager);
        }

        // Command bus registration.
        if (this.commandBusProducer != null) {
            final CommandBus commandBus = this.commandBusProducer.produce(
                    beanManager.createCreationalContext(null));

            logger.info("Registering command bus {}.",
                    commandBus.getClass().getSimpleName());

            configurer.configureCommandBus(c -> commandBus);
        } else {
            logger.info("No command bus producer found, using default simple command bus.");
        }

        // Event handling configuration registration.
        EventHandlingConfiguration eventHandlingConfiguration = new EventHandlingConfiguration();

        for (Producer<ModuleConfiguration> producer : moduleConfigurationProducers) {
            ModuleConfiguration moduleConfiguration = producer.produce(beanManager.createCreationalContext(null));
            logger.info("Registering module configuration {}.", moduleConfiguration.getClass().getSimpleName());
            configurer.registerModule(moduleConfiguration);

            if (moduleConfiguration instanceof EventHandlingConfiguration) {
                eventHandlingConfiguration = (EventHandlingConfiguration) moduleConfiguration;
            }
        }

        // Register event handlers.
        for (Bean<?> eventHandler : eventHandlers) {
            logger.info("Registering event handler {}.",
                        eventHandler.getBeanClass().getName());

            eventHandlingConfiguration.registerEventHandler(
                    c -> eventHandler.create(
                            beanManager.createCreationalContext(null)));
        }

        // Event bus registration.
        if (this.eventBusProducer != null) {
            final EventBus eventBus = this.eventBusProducer.produce(
                    beanManager.createCreationalContext(null));

            logger.info("Registering event bus {}.",
                    eventBus.getClass().getSimpleName());

            configurer.configureEventBus(c -> eventBus);
        }

        // Token store registration.
        if (this.tokenStoreProducer != null) {
            final TokenStore tokenStore = this.tokenStoreProducer.produce(
                    beanManager.createCreationalContext(null));

            logger.info("Registering token store {}.",
                    tokenStore.getClass().getSimpleName());

            configurer.registerComponent(TokenStore.class, c -> tokenStore);
        }

        // Listener invocation error handler registration.
        if (this.listenerInvocationErrorHandlerProducer != null) {
            ListenerInvocationErrorHandler listenerInvocationErrorHandler = this.listenerInvocationErrorHandlerProducer
                    .produce(beanManager.createCreationalContext(null));

            logger.info("Registering listener invocation error handler {}.",
                        listenerInvocationErrorHandler.getClass().getSimpleName());

            configurer.registerComponent(ListenerInvocationErrorHandler.class, c -> listenerInvocationErrorHandler);
        }

        // Error handler registration.
        if (this.errorHandlerProducer != null) {
            ErrorHandler errorHandler = this.errorHandlerProducer
                    .produce(beanManager.createCreationalContext(null));

            logger.info("Registering error handler {}.",
                        errorHandler.getClass().getSimpleName());

            configurer.registerComponent(ErrorHandler.class, c -> errorHandler);
        }

        // Correlation data providers registration
        List<CorrelationDataProvider> correlationDataProviders = this.correlationDataProviderProducers
                .stream()
                .map(producer -> {
                    CorrelationDataProvider correlationDataProvider = producer.produce(
                            beanManager.createCreationalContext(null));
                    logger.info("Registering correlation data provider {}.",
                                correlationDataProvider.getClass().getSimpleName());
                    return correlationDataProvider;
                })
                .collect(Collectors.toList());
        configurer.configureCorrelationDataProviders(c -> correlationDataProviders);

        // Event upcasters registration
        this.eventUpcasterProducers
                .forEach(producer -> {
                    EventUpcaster eventUpcaster = producer.produce(beanManager.createCreationalContext(null));
                    logger.info("Registering event upcaster {}.", eventUpcaster.getClass().getSimpleName());
                    configurer.registerEventUpcaster(c -> eventUpcaster);
                });

        // Error handler registration.
        if (this.queryBusProducer != null) {
            QueryBus queryBus = this.queryBusProducer
                    .produce(beanManager.createCreationalContext(null));

            logger.info("Registering query bus {}.",
                        queryBus.getClass().getSimpleName());

            configurer.configureQueryBus(c -> queryBus);
        }

        // Event storage engine registration.
        if (this.eventStorageEngineProducer != null) {
            final EventStorageEngine eventStorageEngine
                    = this.eventStorageEngineProducer.produce(
                            beanManager.createCreationalContext(null));
            logger.info("Registering event storage {}.",
                    eventStorageEngine.getClass().getSimpleName());
            configurer.configureEmbeddedEventStore(c -> eventStorageEngine);
        }

        // Register aggregates.
        aggregates.forEach(aggregate -> {
            logger.info("Registering aggregate {}", aggregate.getSimpleName());
            configurer.configureAggregate(aggregate);
        });

        logger.info("Starting Axon configuration.");
        configuration = configurer.buildConfiguration();
        configuration.start();

        logger.info("Registering Axon APIs with CDI.");
        afterBeanDiscovery.addBean(
                new BeanWrapper<>(Configuration.class, () -> configuration));
        afterBeanDiscovery.addBean(
                new BeanWrapper<>(CommandBus.class, configuration::commandBus));
        afterBeanDiscovery.addBean(
                new BeanWrapper<>(CommandGateway.class, configuration::commandGateway));
        afterBeanDiscovery.addBean(
                new BeanWrapper<>(EventBus.class, configuration::eventBus));

        // If there was no serializer producer, register the serializer as a bean.
        if (this.serializerProducer == null) {
            afterBeanDiscovery.addBean(
                    new BeanWrapper<>(Serializer.class, configuration::serializer));
        }

        logger.info("Axon Framework configuration complete.");
    }

    void beforeShutdown(@Observes @Destroyed(ApplicationScoped.class) final Object event) {
        configuration.shutdown();
    }
}
