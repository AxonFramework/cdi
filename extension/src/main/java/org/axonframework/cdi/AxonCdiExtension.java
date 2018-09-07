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
import org.axonframework.commandhandling.CommandTargetResolver;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.model.GenericJpaRepository;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.lock.LockFactory;
import org.axonframework.common.lock.NullLockFactory;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.AggregateConfigurer;
import org.axonframework.config.Configuration;
import org.axonframework.config.Configurer;
import org.axonframework.config.ConfigurerModule;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.config.EventHandlingConfiguration;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.config.ModuleConfiguration;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.eventhandling.ErrorHandler;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
import org.axonframework.eventhandling.saga.repository.SagaStore;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
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

    private final List<AggregateDefinition> aggregates = new ArrayList<>();
    private final Map<String, Producer<Repository>> aggregateRepositoryProducerMap = new HashMap<>();
    private final Map<String, Producer<SnapshotTriggerDefinition>> snapshotTriggerDefinitionProducerMap = new HashMap<>();
    private final Map<String, Producer<CommandTargetResolver>> commandTargetResolverProducerMap = new HashMap<>();

    private final List<SagaDefinition> sagas = new ArrayList<>();
    private final Map<String, Producer<SagaStore>> sagaStoreProducerMap = new HashMap<>();
    private final Map<String, Producer<SagaConfiguration<?>>> sagaConfigurationProducerMap = new HashMap<>();

    private final List<MessageHandlingBeanDefinition> messageHandlers = new ArrayList<>();

    private Producer<EventStorageEngine> eventStorageEngineProducer;
    private Producer<Serializer> serializerProducer;
    private Producer<Serializer> eventSerializerProducer;
    private Producer<Serializer> messageSerializerProducer;
    private Producer<EventBus> eventBusProducer;
    private Producer<CommandBus> commandBusProducer;
    private Producer<CommandGateway> commandGatewayProducer;
    private Producer<Configurer> configurerProducer;
    private Producer<TransactionManager> transactionManagerProducer;
    private Producer<EntityManagerProvider> entityManagerProviderProducer;
    private Producer<TokenStore> tokenStoreProducer;
    private Producer<ListenerInvocationErrorHandler> listenerInvocationErrorHandlerProducer;
    private Producer<ErrorHandler> errorHandlerProducer;
    private final List<Producer<CorrelationDataProvider>> correlationDataProviderProducers = new ArrayList<>();
    private Producer<QueryBus> queryBusProducer;
    private Producer<QueryGateway> queryGatewayProducer;
    private Producer<QueryUpdateEmitter> queryUpdateEmitterProducer;
    private final List<Producer<ModuleConfiguration>> moduleConfigurationProducers = new ArrayList<>();
    private final List<Producer<ConfigurerModule>> configurerModuleProducers = new ArrayList<>();
    private final List<Producer<EventUpcaster>> eventUpcasterProducers = new ArrayList<>();
    private Producer<DeadlineManager> deadlineManagerProducer;
    private Producer<EventHandlingConfiguration> eventHandlingConfigurationProducer;
    private Producer<EventProcessingConfiguration> eventProcessingConfigurationProducer;

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

        aggregates.add(new AggregateDefinition(clazz));
    }

    <T> void processAggregateRepositoryProducer(
            @Observes final ProcessProducer<T, Repository> processProducer) {
        logger.debug("Found Producer for Repository: {}.", processProducer.getProducer());

        String repositoryName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.aggregateRepositoryProducerMap.put(repositoryName, processProducer.getProducer());
    }

    <T> void processSnapshotTriggerDefinitionProducer(
            @Observes final ProcessProducer<T, SnapshotTriggerDefinition> processProducer) {
        logger.debug("Found Producer for SnapshotTriggerDefinition: {}.", processProducer.getProducer());

        String triggerDefinitionName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.snapshotTriggerDefinitionProducerMap.put(triggerDefinitionName, processProducer.getProducer());
    }

    <T> void processCommandTargetResolverProducer(
            @Observes final ProcessProducer<T, CommandTargetResolver> processProducer) {
        logger.debug("Found Producer for CommandTargetResolver: {}.", processProducer.getProducer());

        String resolverName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.commandTargetResolverProducerMap.put(resolverName, processProducer.getProducer());
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

        String sagaConfigurationName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        sagaConfigurationProducerMap.put(sagaConfigurationName, processProducer.getProducer());
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
                case "messageSerializer":
                    logger.debug("Producer for MessageSerializer found: {}.",
                            processProducer.getProducer());
                    messageSerializerProducer = processProducer.getProducer();
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

        logger.debug("Producer for TokenStore found: {}.", processProducer.getProducer());

        this.tokenStoreProducer = processProducer.getProducer();
    }

    <T> void processErrorHandlerProducer(
            @Observes final ProcessProducer<T, ErrorHandler> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for ErrorHandler found: {}.", processProducer.getProducer());

        this.errorHandlerProducer = processProducer.getProducer();
    }

    <T> void processListenerInvocationErrorHandlerProducer(
            @Observes final ProcessProducer<T, ListenerInvocationErrorHandler> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for ListenerInvocationErrorHandler found: {}.",
                processProducer.getProducer());

        this.listenerInvocationErrorHandlerProducer = processProducer.getProducer();
    }

    <T> void processCorrelationDataProviderProducer(
            @Observes final ProcessProducer<T, CorrelationDataProvider> processProducer) {
        logger.debug("Producer for CorrelationDataProvider found: {}.",
                processProducer.getProducer());

        this.correlationDataProviderProducers.add(processProducer.getProducer());
    }

    <T> void processQueryBusProducer(
            @Observes final ProcessProducer<T, QueryBus> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for QueryBus found: {}.",
                processProducer.getProducer());

        this.queryBusProducer = processProducer.getProducer();
    }

    <T> void processQueryGatewayProducer(
            @Observes final ProcessProducer<T, QueryGateway> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for QueryGateway found: {}.",
                processProducer.getProducer());

        this.queryGatewayProducer = processProducer.getProducer();
    }

    <T> void processQueryUpdateEmitterProducer(
            @Observes final ProcessProducer<T, QueryUpdateEmitter> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for QueryUpdateEmitter found: {}.", processProducer.getProducer());

        this.queryUpdateEmitterProducer = processProducer.getProducer();
    }

    <T> void processDeadlineManagerProducer(
            @Observes final ProcessProducer<T, DeadlineManager> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for DeadlineManager found: {}.", processProducer.getProducer());

        this.deadlineManagerProducer = processProducer.getProducer();
    }

    <T> void processModuleConfigurationProducer(
            @Observes final ProcessProducer<T, ModuleConfiguration> processProducer) {
        logger.debug("Producer for ModuleConfiguration found: {}.",
                processProducer.getProducer());

        this.moduleConfigurationProducers.add(processProducer.getProducer());
    }

    <T> void processEventHandlingConfigurationProducer(
            @Observes final ProcessProducer<T, EventHandlingConfiguration> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for EventHandlingConfiguration found: {}.",
                processProducer.getProducer());

        this.eventHandlingConfigurationProducer = processProducer.getProducer();
    }

    <T> void processEventProcessingConfigurationProducer(
            @Observes final ProcessProducer<T, EventProcessingConfiguration> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for EventProcessingConfiguration found: {}.",
                processProducer.getProducer());

        this.eventProcessingConfigurationProducer = processProducer.getProducer();
    }

    <T> void processConfigurerModuleProducer(
            @Observes final ProcessProducer<T, ConfigurerModule> processProducer) {
        logger.debug("Producer for ConfigurerModule found: {}.", processProducer.getProducer());

        this.configurerModuleProducers.add(processProducer.getProducer());
    }

    <T> void processSagaStoreProducer(
            @Observes final ProcessProducer<T, SagaStore> processProducer) {
        logger.debug("Producer for SagaStore found: {}.", processProducer.getProducer());

        String sagaStoreName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.sagaStoreProducerMap.put(sagaStoreName, processProducer.getProducer());
    }

    <T> void processEventUpcasterProducer(
            @Observes final ProcessProducer<T, EventUpcaster> processProducer) {
        logger.debug("Producer for EventUpcaster found: {}.", processProducer.getProducer());

        this.eventUpcasterProducers.add(processProducer.getProducer());
    }

    /**
     * Scans all beans and collects beans with message handlers.
     *
     * @param processBean bean processing event.
     */
    <T> void processBean(@Observes final ProcessBean<T> processBean) {
        MessageHandlingBeanDefinition.inspect(processBean.getBean())
                .ifPresent(bean -> {
                    logger.debug("Found {}.", bean);
                    messageHandlers.add(bean);
                });
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
            configurer = produce(beanManager, configurerProducer);
        } else {
            configurer = DefaultConfigurer.defaultConfiguration();
        }

        // Entity manager provider registration.
        if (this.entityManagerProviderProducer != null) {
            final EntityManagerProvider entityManagerProvider = produce(beanManager, entityManagerProviderProducer);

            logger.info("Registering entity manager provider {}.", entityManagerProvider.getClass().getSimpleName());

            configurer.registerComponent(EntityManagerProvider.class, c -> entityManagerProvider);
        }

        // Serializer registration.
        if (this.serializerProducer != null) {
            final Serializer serializer = produce(beanManager, serializerProducer);

            logger.info("Registering serializer {}.", serializer.getClass().getSimpleName());

            configurer.configureSerializer(c -> serializer);
        }

        // Event Serializer registration.
        if (this.eventSerializerProducer != null) {
            final Serializer serializer = produce(beanManager, eventSerializerProducer);

            logger.info("Registering event serializer {}.", serializer.getClass().getSimpleName());

            configurer.configureEventSerializer(c -> serializer);
        }

        // Message Serializer registration.
        if (this.messageSerializerProducer != null) {
            final Serializer serializer = produce(beanManager, messageSerializerProducer);

            logger.info("Registering message serializer {}.", serializer.getClass().getSimpleName());

            configurer.configureMessageSerializer(c -> serializer);
        }

        // Transaction manager registration.
        if (this.transactionManagerProducer != null) {
            final TransactionManager transactionManager = produce(beanManager, transactionManagerProducer);

            logger.info("Registering transaction manager {}.", transactionManager.getClass().getSimpleName());

            configurer.configureTransactionManager(c -> transactionManager);
        }

        // Command bus registration.
        if (this.commandBusProducer != null) {
            final CommandBus commandBus = produce(beanManager, commandBusProducer);

            logger.info("Registering command bus {}.", commandBus.getClass().getSimpleName());

            configurer.configureCommandBus(c -> commandBus);
        } else {
            logger.info("No command bus producer found, using default simple command bus.");
        }

        // Module configurations registration.
        // TODO: 8/30/2018 :
        // There is a possible issue with following construct:
        // @Produce
        // public ModuleConfiguration eventHandlingConfiguration() {
        //     return new EventHandlingConfiguration();
        // }
        // It will be registered as module configuration only and not as EventHandlingConfiguration! This will result
        // in having double EventHandlingConfiguration within Configuration. The same applies to EventProcessingConfiguration.
        for (Producer<ModuleConfiguration> producer : moduleConfigurationProducers) {
            configurer.registerModule(new LazyRetrievedModuleConfiguration(() -> {
                ModuleConfiguration moduleConfiguration
                        = producer.produce(beanManager.createCreationalContext(null));
                logger.info("Registering module configuration {}.", moduleConfiguration.getClass().getSimpleName());
                return moduleConfiguration;
            }));
        }

        // Event handling configuration registration.
        EventHandlingConfiguration eventHandlingConfiguration;
        if (eventHandlingConfigurationProducer != null) {
            eventHandlingConfiguration = produce(beanManager, eventHandlingConfigurationProducer);
        } else {
            eventHandlingConfiguration = new EventHandlingConfiguration();
        }
        logger.info("Registering event handling configuration {}.",
                eventHandlingConfiguration.getClass().getSimpleName());
        configurer.registerModule(eventHandlingConfiguration);

        // Event processing configuration registration.
        if (eventProcessingConfigurationProducer != null) {
            EventProcessingConfiguration eventProcessingConfiguration
                    = produce(beanManager, eventProcessingConfigurationProducer);

            logger.info("Registering event processing configuration {}.",
                    eventProcessingConfiguration.getClass().getSimpleName());

            configurer.registerModule(eventProcessingConfiguration);
        }

        // Configurer modules registration
        configurerModuleProducers.forEach(producer -> {
            ConfigurerModule configurerModule = produce(beanManager, producer);
            logger.info("Configuring module {}.", configurerModule.getClass().getSimpleName());
            configurerModule.configureModule(configurer);
        });

        registerMessageHandlers(beanManager, configurer, eventHandlingConfiguration);

        // Event bus registration.
        if (this.eventBusProducer != null) {
            final EventBus eventBus = produce(beanManager, eventBusProducer);

            logger.info("Registering event bus {}.", eventBus.getClass().getSimpleName());

            configurer.configureEventBus(c -> eventBus);
        }

        // Token store registration.
        if (this.tokenStoreProducer != null) {
            final TokenStore tokenStore = produce(beanManager, tokenStoreProducer);

            logger.info("Registering token store {}.", tokenStore.getClass().getSimpleName());

            configurer.registerComponent(TokenStore.class, c -> tokenStore);
        }

        // Listener invocation error handler registration.
        if (this.listenerInvocationErrorHandlerProducer != null) {
            ListenerInvocationErrorHandler listenerInvocationErrorHandler
                    = produce(beanManager, listenerInvocationErrorHandlerProducer);

            logger.info("Registering listener invocation error handler {}.",
                    listenerInvocationErrorHandler.getClass().getSimpleName());

            configurer.registerComponent(ListenerInvocationErrorHandler.class, c -> listenerInvocationErrorHandler);
        }

        // Error handler registration.
        if (this.errorHandlerProducer != null) {
            ErrorHandler errorHandler = produce(beanManager, errorHandlerProducer);

            logger.info("Registering error handler {}.", errorHandler.getClass().getSimpleName());

            configurer.registerComponent(ErrorHandler.class, c -> errorHandler);
        }

        // Correlation data providers registration
        List<CorrelationDataProvider> correlationDataProviders = this.correlationDataProviderProducers
                .stream()
                .map(producer -> {
                    CorrelationDataProvider correlationDataProvider = produce(beanManager, producer);
                    logger.info("Registering correlation data provider {}.",
                            correlationDataProvider.getClass().getSimpleName());
                    return correlationDataProvider;
                })
                .collect(Collectors.toList());
        configurer.configureCorrelationDataProviders(c -> correlationDataProviders);

        // Event upcasters registration
        this.eventUpcasterProducers
                .forEach(producer -> {
                    EventUpcaster eventUpcaster = produce(beanManager, producer);
                    logger.info("Registering event upcaster {}.", eventUpcaster.getClass().getSimpleName());
                    configurer.registerEventUpcaster(c -> eventUpcaster);
                });

        // QueryBus registration.
        if (this.queryBusProducer != null) {
            QueryBus queryBus = produce(beanManager, queryBusProducer);

            logger.info("Registering query bus {}.", queryBus.getClass().getSimpleName());

            configurer.configureQueryBus(c -> queryBus);
        }

        // QueryGateway registration.
        if (this.queryGatewayProducer != null) {
            QueryGateway queryGateway = produce(beanManager, queryGatewayProducer);

            logger.info("Registering query gateway {}.", queryGateway.getClass().getSimpleName());

            configurer.registerComponent(QueryGateway.class, c -> queryGateway);
        }

        // QueryUpdateEmitter registration.
        if (this.queryUpdateEmitterProducer != null) {
            QueryUpdateEmitter queryUpdateEmitter = produce(beanManager, queryUpdateEmitterProducer);

            logger.info("Registering query update emitter {}.", queryUpdateEmitter.getClass().getSimpleName());

            configurer.configureQueryUpdateEmitter(c -> queryUpdateEmitter);
        }

        // DeadlineManager registration.
        if (this.deadlineManagerProducer != null) {
            DeadlineManager deadlineManager = produce(beanManager, deadlineManagerProducer);

            logger.info("Registering deadline manager {}.", deadlineManager.getClass().getSimpleName());

            configurer.registerComponent(DeadlineManager.class, c -> deadlineManager);
        }

        // Event storage engine registration.
        if (this.eventStorageEngineProducer != null) {
            final EventStorageEngine eventStorageEngine = produce(beanManager, eventStorageEngineProducer);

            logger.info("Registering event storage {}.", eventStorageEngine.getClass().getSimpleName());

            configurer.configureEmbeddedEventStore(c -> eventStorageEngine);
        }

        registerAggregates(beanManager, configurer);
        registerSagaStore(beanManager, configurer);
        registerSagas(beanManager, afterBeanDiscovery, configurer);

        logger.info("Starting Axon configuration.");
        configuration = configurer.start();

        logger.info("Registering Axon APIs with CDI.");
        afterBeanDiscovery.addBean(
                new BeanWrapper<>(Configuration.class, () -> configuration));
        addIfNotConfigured(CommandBus.class, commandBusProducer,
                configuration::commandBus, afterBeanDiscovery);
        addIfNotConfigured(CommandGateway.class,
                commandGatewayProducer,
                configuration::commandGateway,
                afterBeanDiscovery);
        addIfNotConfigured(QueryBus.class, queryBusProducer, configuration::queryBus, afterBeanDiscovery);
        addIfNotConfigured(QueryGateway.class, queryGatewayProducer, configuration::queryGateway, afterBeanDiscovery);
        addIfNotConfigured(QueryUpdateEmitter.class,
                queryUpdateEmitterProducer,
                configuration::queryUpdateEmitter,
                afterBeanDiscovery);
        addIfNotConfigured(EventBus.class, eventBusProducer, configuration::eventBus, afterBeanDiscovery);
        addIfNotConfigured(Serializer.class, serializerProducer, configuration::serializer, afterBeanDiscovery);

        logger.info("Axon Framework configuration complete.");
    }

    void beforeShutdown(@Observes @Destroyed(ApplicationScoped.class) final Object event) {
        configuration.shutdown();
    }

    private void registerMessageHandlers(BeanManager beanManager, Configurer configurer,
            EventHandlingConfiguration eventHandlingConfiguration) {
        for (MessageHandlingBeanDefinition messageHandler : messageHandlers) {
            // TODO: 8/31/2018 should we always create instances? or should we check whether it already exists?
            Object instance = messageHandler.getBean().create(beanManager.createCreationalContext(null));
            if (messageHandler.isEventHandler()) {
                logger.info("Registering event handler {}.", instance.getClass().getSimpleName());
                eventHandlingConfiguration.registerEventHandler(c -> instance);
            }
            if (messageHandler.isCommandHandler()) {
                logger.info("Registering command handler {}.", instance.getClass().getSimpleName());
                configurer.registerCommandHandler(c -> instance);
            }
            if (messageHandler.isQueryHandler()) {
                logger.info("Registering query handler {}.", instance.getClass().getSimpleName());
                configurer.registerQueryHandler(c -> instance);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerAggregates(BeanManager beanManager, Configurer configurer) {
        aggregates.forEach(aggregateDefinition -> {
            logger.info("Registering aggregate {}.", aggregateDefinition.aggregateType().getSimpleName());

            AggregateConfigurer<?> aggregateConf = AggregateConfigurer.defaultConfiguration(aggregateDefinition.aggregateType());
            if (aggregateDefinition.repository().isPresent()) {
                aggregateConf.configureRepository(
                        c -> produce(beanManager, aggregateRepositoryProducerMap
                                .get(aggregateDefinition.repository().get())));
            } else {
                if (aggregateRepositoryProducerMap.containsKey(aggregateDefinition.repositoryName())) {
                    aggregateConf.configureRepository(
                            c -> produce(beanManager, aggregateRepositoryProducerMap
                                    .get(aggregateDefinition.repositoryName())));
                } else {
                    // TODO: 8/29/2018 check how to do in CDI world: register repository as a bean
                    // TODO: 8/29/2018 check how to do in CDI world: aggregate factory
                    aggregateDefinition.snapshotTriggerDefinition().ifPresent(triggerDefinition -> aggregateConf
                            .configureSnapshotTrigger(
                                    c -> produce(beanManager, snapshotTriggerDefinitionProducerMap
                                            .get(triggerDefinition))));
                    if (aggregateDefinition.isJpaAggregate()) {
                        aggregateConf.configureRepository(
                                c -> new GenericJpaRepository(
                                        // TODO: 8/29/2018 what to do about default EntityManagerProvider (check spring impl)
                                        c.getComponent(EntityManagerProvider.class),
                                        aggregateDefinition.aggregateType(),
                                        c.eventBus(),
                                        c::repository,
                                        c.getComponent(LockFactory.class, () -> NullLockFactory.INSTANCE),
                                        c.parameterResolverFactory(),
                                        c.handlerDefinition(aggregateDefinition.aggregateType())));
                    }
                }
            }

            if (aggregateDefinition.commandTargetResolver().isPresent()) {
                aggregateConf.configureCommandTargetResolver(
                        c -> produce(beanManager,
                                commandTargetResolverProducerMap.get(aggregateDefinition.commandTargetResolver().get())));
            } else {
                commandTargetResolverProducerMap.keySet()
                        .stream()
                        .filter(resolver -> aggregates.stream()
                        .filter(a -> a.commandTargetResolver().isPresent())
                        .map(a -> a.commandTargetResolver().get())
                        .noneMatch(resolver::equals))
                        .findFirst() // TODO: 8/29/2018 what if there are more "default" resolvers
                        .ifPresent(
                                resolver -> aggregateConf.configureCommandTargetResolver(
                                        c -> produce(beanManager,
                                                commandTargetResolverProducerMap.get(resolver))));
            }

            configurer.configureAggregate(aggregateConf);
        });
    }

    private void registerSagaStore(BeanManager beanManager, Configurer configurer) {
        sagaStoreProducerMap.keySet()
                .stream()
                .filter(storeName -> sagas.stream()
                .filter(sd -> sd.sagaStore().isPresent())
                .map(sd -> sd.sagaStore().get())
                .noneMatch(storeName::equals))
                .findFirst() // TODO: 8/29/2018 what if there are more "default" saga stores???
                .ifPresent(storeName -> {
                    SagaStore sagaStore = produce(beanManager, sagaStoreProducerMap.get(storeName));
                    logger.info("Registering saga store {}.", sagaStore.getClass().getSimpleName());
                    configurer.registerComponent(SagaStore.class, c -> sagaStore);
                });
    }

    @SuppressWarnings("unchecked")
    private void registerSagas(BeanManager beanManager, AfterBeanDiscovery afterBeanDiscovery, Configurer configurer) {
        sagas.forEach(sagaDefinition -> {
            logger.info("Registering saga {}.", sagaDefinition.sagaType().getSimpleName());

            if (!sagaDefinition.explicitConfiguration()
                    && !sagaConfigurationProducerMap.containsKey(sagaDefinition.configurationName())) {

                SagaConfiguration<?> sagaConfiguration = SagaConfiguration
                        .subscribingSagaManager(sagaDefinition.sagaType());

                afterBeanDiscovery.addBean(new BeanWrapper<>(sagaDefinition.configurationName(),
                        SagaConfiguration.class,
                        () -> sagaConfiguration));

                sagaDefinition.sagaStore()
                        .ifPresent(sagaStore -> sagaConfiguration
                        .configureSagaStore(c -> produce(beanManager, sagaStoreProducerMap.get(sagaStore))));
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

    // TODO: 8/29/2018 should we store produced components in some kind of cache and extract them if present?
    private <T> T produce(BeanManager beanManager, Producer<T> producer) {
        // Antoine: Is createCreationalContext(null) is correct here?
        // If not, what should I do instead? Again, many of these things
        // may be indirectly referencing container resources.
        return producer.produce(beanManager.createCreationalContext(null));
    }
}
