package org.axonframework.cdi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
import org.axonframework.cdi.stereotype.Saga;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.axonframework.config.ModuleConfiguration;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.ErrorHandler;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
import org.axonframework.eventhandling.saga.repository.SagaStore;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.EventUpcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main CDI extension class responsible for collecting CDI beans and setting up
 * Axon configuration.
 */
public class AxonCdiExtension implements Extension {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private Configuration configuration;

    private final List<Class<?>> aggregates = new ArrayList<>();
    private final List<SagaDefinition> sagas = new ArrayList<>();
    private final List<Bean<?>> eventHandlers = new ArrayList<>();
    private final List<Bean<?>> queryHandlers = new ArrayList<>();
    private final List<Bean<?>> projections = new ArrayList<>();

    private Producer<EventStorageEngine> eventStorageEngineProducer;
    private Producer<Serializer> serializerProducer;
    private Producer<Serializer> eventSerializerProducer;
    private Producer<EventBus> eventBusProducer;
    private Producer<CommandBus> commandBusProducer;
    private Producer<CommandGateway> commandGatewayProducer;
    private Producer<Configurer> configurerProducer;
    private Producer<TransactionManager> transactionManagerProducer;
    private Producer<EntityManagerProvider> entityManagerProviderProducer;
    private Producer<TokenStore> tokenStoreProducer;
    private Producer<ListenerInvocationErrorHandler> listenerInvocationErrorHandlerProducer;
    private Producer<ErrorHandler> errorHandlerProducer;
    private final List<Producer<CorrelationDataProvider>> correlationDataProviderProducers
            = new ArrayList<>();
    private Producer<QueryBus> queryBusProducer;
    private Producer<QueryGateway> queryGatewayProducer;
    private final List<Producer<ModuleConfiguration>> moduleConfigurationProducers
            = new ArrayList<>();
    private final List<Producer<EventUpcaster>> eventUpcasterProducers
            = new ArrayList<>();
    private final Map<String, Producer<SagaStore>> sagaStoreProducerMap = new HashMap<>();
    private final Map<String, Producer<SagaConfiguration<?>>> sagaConfigurationProducerMap = new HashMap<>();

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

    <T> void processSaga(@Observes @WithAnnotations({Saga.class})
                              final ProcessAnnotatedType<T> processAnnotatedType) {
        final Class<?> clazz = processAnnotatedType.getAnnotatedType().getJavaClass();

        logger.debug("Found Saga: {}.", clazz);

        sagas.add(new SagaDefinition(clazz));
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
     */
    <T> void processEventStorageEngineProducer(
            @Observes final ProcessProducer<T, EventStorageEngine> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for EventStorageEngine found: {}.",
                processProducer.getProducer());

        this.eventStorageEngineProducer = processProducer.getProducer();
    }

    <T> void processSagaConfigurationProducer(
            @Observes final ProcessProducer<T, SagaConfiguration<?>> processProducer) {
        logger.debug("Producer for SagaConfiguration found: {}.",
                     processProducer.getProducer());

        String sagaConfigName = extractBeanName(processProducer.getAnnotatedMember());

        sagaConfigurationProducerMap.put(sagaConfigName, processProducer.getProducer());
    }

    /**
     * Scans for a configurer producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processConfigurerProducer(
            @Observes final ProcessProducer<T, Configurer> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for Configurer found: {}.", processProducer.getProducer());

        this.configurerProducer = processProducer.getProducer();
    }

    /**
     * Scans for a transaction manager producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processTransactionManagerProducer(
            @Observes final ProcessProducer<T, TransactionManager> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for TransactionManager found: {}.",
                processProducer.getProducer());

        this.transactionManagerProducer = processProducer.getProducer();
    }

    /**
     * Scans for a serializer producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processSerializerProducer(
            @Observes final ProcessProducer<T, Serializer> processProducer) {
        // TODO Handle multiple serializer definitions of the same type.

        AnnotatedMember<T> annotatedMember = processProducer.getAnnotatedMember();
        Named named = annotatedMember.getAnnotation(Named.class);

        if (named != null) {
            String namedValue = named.value();
            String serializerName = "".equals(namedValue)
                    ? annotatedMember.getJavaMember().getName()
                    : namedValue;
            switch (serializerName) {
                case "eventSerializer":
                    logger.debug("Producer for EventSerializer found: {}.",
                            processProducer.getProducer());
                    eventSerializerProducer = processProducer.getProducer();
                    break;
                case "serializer":
                    logger.debug("Producer for Serializer found: {}.",
                            processProducer.getProducer());
                    this.serializerProducer = processProducer.getProducer();
                    break;
                default:
                    logger.warn("Unknown named serializer configured: " + serializerName);
            }
        } else {
            logger.debug("Producer for Serializer found: {}.", processProducer.getProducer());
            this.serializerProducer = processProducer.getProducer();
        }
    }

    /**
     * Scans for an event bus producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processEventBusProducer(
            @Observes final ProcessProducer<T, EventBus> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for EventBus found: {}.", processProducer.getProducer());

        this.eventBusProducer = processProducer.getProducer();
    }

    /**
     * Scans for a command bus producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processCommandBusProducer(
            @Observes final ProcessProducer<T, CommandBus> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for CommandBus found: {}.",
                processProducer.getProducer());

        this.commandBusProducer = processProducer.getProducer();
    }

    <T> void processCommandGatewayProducer(
            @Observes final ProcessProducer<T, CommandGateway> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for CommandGateway found: {}.",
                     processProducer.getProducer());

        this.commandGatewayProducer = processProducer.getProducer();
    }

    /**
     * Scans for an entity manager provider producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processEntityManagerProviderProducer(
            @Observes final ProcessProducer<T, EntityManagerProvider> processProducer) {
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
     */
    <T> void processTokenStoreProducer(
            @Observes final ProcessProducer<T, TokenStore> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for TokenStore: {}.", processProducer.getProducer());

        this.tokenStoreProducer = processProducer.getProducer();
    }

    <T> void processErrorHandlerProducer(
            @Observes final ProcessProducer<T, ErrorHandler> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for ErrorHandler: {}.", processProducer.getProducer());

        this.errorHandlerProducer = processProducer.getProducer();
    }

    <T> void processListenerInvocationErrorHandlerProducer(
            @Observes final ProcessProducer<T, ListenerInvocationErrorHandler> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for ListenerInvocationErrorHandler: {}.",
                processProducer.getProducer());

        this.listenerInvocationErrorHandlerProducer = processProducer.getProducer();
    }

    <T> void processCorrelationDataProviderProducer(
            @Observes final ProcessProducer<T, CorrelationDataProvider> processProducer) {
        logger.debug("Producer for CorrelationDataProvider: {}.", processProducer.getProducer());

        this.correlationDataProviderProducers.add(processProducer.getProducer());
    }

    <T> void processQueryBusProducer(
            @Observes final ProcessProducer<T, QueryBus> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for QueryBus: {}.", processProducer.getProducer());

        this.queryBusProducer = processProducer.getProducer();
    }

    <T> void processQueryGatewayProducer(
            @Observes final ProcessProducer<T, QueryGateway> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for QueryGateway: {}.", processProducer.getProducer());

        this.queryGatewayProducer = processProducer.getProducer();
    }

    <T> void processModuleConfigurationProducer(
            @Observes final ProcessProducer<T, ModuleConfiguration> processProducer) {
        logger.debug("Producer for ModuleConfiguration: {}.", processProducer.getProducer());

        this.moduleConfigurationProducers.add(processProducer.getProducer());
    }

    <T> void processSagaStoreProducer(
            @Observes final ProcessProducer<T, SagaStore> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for SagaSTore: {}.", processProducer.getProducer());

        String sagaStoreName = extractBeanName(processProducer.getAnnotatedMember());

        this.sagaStoreProducerMap.put(sagaStoreName, processProducer.getProducer());
    }

    <T> void processEventUpcasterProducer(
            @Observes final ProcessProducer<T, EventUpcaster> processProducer) {
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

        boolean isEventHandler = CdiUtilities.hasAnnotatedMethod(bean, EventHandler.class);
        boolean isQueryHandler = CdiUtilities.hasAnnotatedMethod(bean, QueryHandler.class);

        if (isEventHandler && isQueryHandler) {
            logger.debug("Found projection {}.", bean.getBeanClass().getSimpleName());
            projections.add(bean);
        } else if (isEventHandler) {
            logger.debug("Found event handler {}.", bean.getBeanClass().getSimpleName());
            eventHandlers.add(bean);
        } else if (isQueryHandler) {
            logger.debug("Found query handler {}.", bean.getBeanClass().getSimpleName());
            queryHandlers.add(bean);
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
        EventHandlingConfiguration eventHandlingConfiguration = null;

        for (Producer<ModuleConfiguration> producer : moduleConfigurationProducers) {
            ModuleConfiguration moduleConfiguration
                    = producer.produce(beanManager.createCreationalContext(null));
            logger.info("Registering module configuration {}.",
                    moduleConfiguration.getClass().getSimpleName());
            configurer.registerModule(moduleConfiguration);

            if (moduleConfiguration instanceof EventHandlingConfiguration) {
                eventHandlingConfiguration = (EventHandlingConfiguration) moduleConfiguration;
            }
        }

        if (eventHandlingConfiguration == null) {
            eventHandlingConfiguration = new EventHandlingConfiguration();
            configurer.registerModule(eventHandlingConfiguration);
        }

        // Register projections.
        for (Bean<?> projection : projections) {
            logger.info("Registering projection {}.", projection.getBeanClass().getName());
            Object projectionInstance = projection.create(beanManager.createCreationalContext(null));
            eventHandlingConfiguration.registerEventHandler(c -> projectionInstance);
            configurer.registerQueryHandler(c -> projectionInstance);
        }

        // Register event handlers.
        for (Bean<?> eventHandler : eventHandlers) {
            logger.info("Registering event handler {}.",
                    eventHandler.getBeanClass().getName());

            eventHandlingConfiguration.registerEventHandler(
                    c -> eventHandler.create(
                            beanManager.createCreationalContext(null)));
        }

        // Register query handlers.
        for (Bean<?> queryHandler : queryHandlers) {
            logger.info("Registering query handler {}.", queryHandler.getBeanClass().getName());

            configurer.registerQueryHandler(c -> queryHandler.create(
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
            ListenerInvocationErrorHandler listenerInvocationErrorHandler
                    = this.listenerInvocationErrorHandlerProducer
                            .produce(beanManager.createCreationalContext(null));

            logger.info("Registering listener invocation error handler {}.",
                    listenerInvocationErrorHandler.getClass().getSimpleName());

            configurer.registerComponent(ListenerInvocationErrorHandler.class,
                    c -> listenerInvocationErrorHandler);
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

        registerSagaStore(beanManager, configurer);

        // Register aggregates.
        aggregates.forEach(aggregateType -> {
            logger.info("Registering aggregate {}.", aggregateType.getSimpleName());
            configurer.configureAggregate(aggregateType);
        });

        registerSagas(beanManager, afterBeanDiscovery, configurer);

        logger.info("Starting Axon configuration.");
        configuration = configurer.buildConfiguration();
        configuration.start();

        logger.info("Registering Axon APIs with CDI.");

        afterBeanDiscovery.addBean(
                new BeanWrapper<>(Configuration.class, () -> configuration));
        addIfNotConfigured(CommandBus.class, commandBusProducer, configuration::commandBus, afterBeanDiscovery);
        addIfNotConfigured(CommandGateway.class,
                           commandGatewayProducer,
                           configuration::commandGateway,
                           afterBeanDiscovery);
        addIfNotConfigured(QueryBus.class, queryBusProducer, configuration::queryBus, afterBeanDiscovery);
        addIfNotConfigured(QueryGateway.class, queryGatewayProducer, configuration::queryGateway, afterBeanDiscovery);
        addIfNotConfigured(EventBus.class, eventBusProducer, configuration::eventBus, afterBeanDiscovery);
        addIfNotConfigured(Serializer.class, serializerProducer, configuration::serializer, afterBeanDiscovery);

        logger.info("Axon Framework configuration complete.");
    }

    void beforeShutdown(@Observes @Destroyed(ApplicationScoped.class) final Object event) {
        configuration.shutdown();
    }

    private void registerSagaStore(BeanManager beanManager, Configurer configurer) {
        sagaStoreProducerMap.keySet()
                            .stream()
                            .filter(storeName -> sagas.stream()
                                                      .filter(sd -> sd.sagaStore().isPresent())
                                                      .map(sd -> sd.sagaStore().get())
                                                      .noneMatch(storeName::equals))
                            .findFirst()
                            .ifPresent(storeName -> {
                                SagaStore sagaStore = sagaStoreProducerMap.get(storeName)
                                                                          .produce(beanManager
                                                                                           .createCreationalContext(null));
                                logger.info("Registering saga store {}.", sagaStore.getClass().getSimpleName());
                                configurer.registerComponent(SagaStore.class, c -> sagaStore);
                            });
    }

    private void registerSagas(BeanManager beanManager, AfterBeanDiscovery afterBeanDiscovery, Configurer configurer) {
        sagas.forEach(sagaDefinition -> {
            logger.info("Registering saga {}.", sagaDefinition.sagaType().getSimpleName());

            if (!sagaDefinition.explicitConfiguration() &&
                    !sagaConfigurationProducerMap.containsKey(sagaDefinition.configurationName())) {

                SagaConfiguration<?> sagaConfiguration = SagaConfiguration
                        .subscribingSagaManager(sagaDefinition.sagaType());

                afterBeanDiscovery.addBean(new BeanWrapper<>(sagaDefinition.configurationName(),
                                                             SagaConfiguration.class,
                                                             () -> sagaConfiguration));

                sagaDefinition.sagaStore()
                              .ifPresent(sagaStore -> sagaConfiguration
                                      .configureSagaStore(c -> sagaStoreProducerMap.get(sagaStore)
                                                                                   .produce(beanManager
                                                                                                    .createCreationalContext(
                                                                                                            null))));
                configurer.registerModule(sagaConfiguration);
            }
        });
    }

    private <T> void addIfNotConfigured(Class<T> componentType, Producer<T> componentProducer,
                                        Supplier<T> componentSupplier, AfterBeanDiscovery afterBeanDiscovery) {
        if (componentProducer == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(componentType, componentSupplier));
        }
    }

    private String extractBeanName(AnnotatedMember<?> annotatedMember) {
        String beanName = annotatedMember.getJavaMember().getName();
        Named named = annotatedMember.getAnnotation(Named.class);
        if (named != null && !"".equals(named.value())) {
            beanName = named.value();
        }
        return beanName;
    }
}
