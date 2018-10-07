package org.axonframework.cdi.config;

import org.axonframework.cdi.BeanWrapper;
import org.axonframework.cdi.CdiUtilities;
import org.axonframework.cdi.LazyRetrievedModuleConfiguration;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.*;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.eventhandling.ErrorHandler;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.EventUpcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Producer;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.axonframework.cdi.AxonCdiExtension.produce;

public class AxonCdiConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private Producer<EventStorageEngine> eventStorageEngineProducer;
    private Producer<Serializer> serializerProducer;
    private Producer<Serializer> eventSerializerProducer;
    private Producer<Serializer> messageSerializerProducer;
    private Producer<EventBus> eventBusProducer;
    private Producer<CommandBus> commandBusProducer;
    //     private Producer<CommandGateway> commandGatewayProducer;
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

    private EventHandlingConfiguration eventHandlingConfiguration = null;

    public Configurer produceBeans(BeanManager beanManager) {
        Configurer configurer;

        if (this.configurerProducer != null) {
            // Mark: this is one of the biggest headaches I am facing. What I
            // need is the actual bean instance that I need to move forward with.
            // Right now what I am doing is just having the producer create it.
            // The problem with this is that CDI and the Java EE runtime is just
            // not fullly ready to do that at this stage. In addition, the
            // bean may be referencing definitions I have not had a chance
            // to register with CDI yet since they can only be produced after the
            // Axon configuration is done.
            //
            // The only solution I can think of is register any bean
            // definitions I've detected I absolutely need and avoid actual
            // bean reference until later, create the beans like configurer
            // that I say in the API cannot have circular dependencies and move the
            // configuration steps itself to be as lazily loaded as possible.
            // Can you think of a better strategy to deal with this problem?
            configurer = produce(beanManager, configurerProducer);
            logger.info("Starting with an application provided configurer: {}.",
                    configurer.getClass().getSimpleName());
        } else {
            logger.info("Starting with the Axon default configuration.");

            configurer = DefaultConfigurer.defaultConfiguration();
        }

        if (this.entityManagerProviderProducer != null) {
            final EntityManagerProvider entityManagerProvider
                    = produce(beanManager, entityManagerProviderProducer);

            logger.info("Registering entity manager provider: {}.",
                    entityManagerProvider.getClass().getSimpleName());

            configurer.registerComponent(EntityManagerProvider.class, c -> entityManagerProvider);
        }

        if (this.serializerProducer != null) {
            final Serializer serializer = produce(beanManager, serializerProducer);

            logger.info("Registering serializer: {}.", serializer.getClass().getSimpleName());

            configurer.configureSerializer(c -> serializer);
        }

        if (this.eventSerializerProducer != null) {
            final Serializer serializer = produce(beanManager, eventSerializerProducer);

            logger.info("Registering event serializer: {}.", serializer.getClass().getSimpleName());

            configurer.configureEventSerializer(c -> serializer);
        }

        if (this.messageSerializerProducer != null) {
            final Serializer serializer = produce(beanManager, messageSerializerProducer);

            logger.info("Registering message serializer: {}.", serializer.getClass().getSimpleName());

            configurer.configureMessageSerializer(c -> serializer);
        }

        if (this.transactionManagerProducer != null) {
            final TransactionManager transactionManager
                    = produce(beanManager, transactionManagerProducer);

            logger.info("Registering transaction manager: {}.", transactionManager.getClass().getSimpleName());

            configurer.configureTransactionManager(c -> transactionManager);
        }

        if (this.commandBusProducer != null) {
            final CommandBus commandBus = produce(beanManager, commandBusProducer);

            logger.info("Registering command bus: {}.", commandBus.getClass().getSimpleName());

            configurer.configureCommandBus(c -> commandBus);
        }

        // Module configurations registration.
        // TODO: 8/30/2018 :
        // There is a possible issue with following construct:
        // @Produce
        // public ModuleConfiguration eventHandlingConfiguration() {
        //     return new EventHandlingConfiguration();
        // }
        // It will be registered as module configuration only and not as EventHandlingConfiguration!
        // This will result in having double EventHandlingConfiguration within Configuration.
        // The same applies to EventProcessingConfiguration.
        // Milan: I need to understand this a bit more from you, but I think the solution
        // is to simply mandate the presence of @Named for EventHandlingConfiguration
        // and EventProcessingConfiguration that is produced as type ModuleConfiguration
        // but is not really a module configuration.
        for (Producer<ModuleConfiguration> producer : moduleConfigurationProducers) {
            // Milan: This looks very interesting. I would like to understand why
            // we are doing this and whether we should do this more globally,
            // perhaps moving the entire Axon configuration to AfterDeploymentValidation
            // instead of AfterBeanDiscovery.
            // Antoine: Do you have any thoughts on this? Could this approach
            // help solve any potential initialization issues? What's the easiest
            // way to do this if so?
            configurer.registerModule(new LazyRetrievedModuleConfiguration(() -> {
                ModuleConfiguration moduleConfiguration
                        = producer.produce(beanManager.createCreationalContext(null));
                logger.info("Registering module configuration: {}.",
                        moduleConfiguration.getClass().getSimpleName());
                return moduleConfiguration;
            }));
        }

        if (eventHandlingConfigurationProducer != null) {
            eventHandlingConfiguration = produce(beanManager, eventHandlingConfigurationProducer);
        } else {
            eventHandlingConfiguration = new EventHandlingConfiguration();
        }

        logger.info("Registering event handling configuration: {}.",
                eventHandlingConfiguration.getClass().getSimpleName());
        configurer.registerModule(eventHandlingConfiguration);

        if (eventProcessingConfigurationProducer != null) {
            EventProcessingConfiguration eventProcessingConfiguration
                    = produce(beanManager, eventProcessingConfigurationProducer);

            logger.info("Registering event processing configuration: {}.",
                    eventProcessingConfiguration.getClass().getSimpleName());

            configurer.registerModule(eventProcessingConfiguration);
        }

        configurerModuleProducers.forEach(producer -> {
            ConfigurerModule configurerModule = produce(beanManager, producer);

            logger.info("Configuring module: {}.", configurerModule.getClass().getSimpleName());

            configurerModule.configureModule(configurer);
        });

        if (this.eventBusProducer != null) {
            final EventBus eventBus = produce(beanManager, eventBusProducer);

            logger.info("Registering event bus: {}.", eventBus.getClass().getSimpleName());

            configurer.configureEventBus(c -> eventBus);
        }

        if (this.tokenStoreProducer != null) {
            final TokenStore tokenStore = produce(beanManager, tokenStoreProducer);

            logger.info("Registering token store: {}.", tokenStore.getClass().getSimpleName());

            configurer.registerComponent(TokenStore.class, c -> tokenStore);
        }

        if (this.listenerInvocationErrorHandlerProducer != null) {
            ListenerInvocationErrorHandler listenerInvocationErrorHandler
                    = produce(beanManager, listenerInvocationErrorHandlerProducer);

            logger.info("Registering listener invocation error handler: {}.",
                    listenerInvocationErrorHandler.getClass().getSimpleName());

            configurer.registerComponent(ListenerInvocationErrorHandler.class,
                    c -> listenerInvocationErrorHandler);
        }

        if (this.errorHandlerProducer != null) {
            ErrorHandler errorHandler = produce(beanManager, errorHandlerProducer);

            logger.info("Registering error handler: {}.", errorHandler.getClass().getSimpleName());

            configurer.registerComponent(ErrorHandler.class, c -> errorHandler);
        }

        // Correlation data providers registration
        List<CorrelationDataProvider> correlationDataProviders = this.correlationDataProviderProducers
                .stream()
                .map(producer -> {
                    CorrelationDataProvider correlationDataProvider = produce(beanManager, producer);

                    logger.info("Registering correlation data provider: {}.",
                            correlationDataProvider.getClass().getSimpleName());

                    return correlationDataProvider;
                })
                .collect(Collectors.toList());
        configurer.configureCorrelationDataProviders(c -> correlationDataProviders);

        this.eventUpcasterProducers
                .forEach(producer -> {
                    EventUpcaster eventUpcaster = produce(beanManager, producer);
                    logger.info("Registering event upcaster: {}.",
                            eventUpcaster.getClass().getSimpleName());
                    configurer.registerEventUpcaster(c -> eventUpcaster);
                });

        if (this.queryBusProducer != null) {
            QueryBus queryBus = produce(beanManager, queryBusProducer);

            logger.info("Registering query bus: {}.", queryBus.getClass().getSimpleName());

            configurer.configureQueryBus(c -> queryBus);
        }

        if (this.queryGatewayProducer != null) {
            QueryGateway queryGateway = produce(beanManager, queryGatewayProducer);

            logger.info("Registering query gateway: {}.", queryGateway.getClass().getSimpleName());

            configurer.registerComponent(QueryGateway.class, c -> queryGateway);
        }

        if (this.queryUpdateEmitterProducer != null) {
            QueryUpdateEmitter queryUpdateEmitter = produce(beanManager, queryUpdateEmitterProducer);

            logger.info("Registering query update emitter: {}.", queryUpdateEmitter.getClass().getSimpleName());

            configurer.configureQueryUpdateEmitter(c -> queryUpdateEmitter);
        }

        if (this.deadlineManagerProducer != null) {
            DeadlineManager deadlineManager = produce(beanManager, deadlineManagerProducer);

            logger.info("Registering deadline manager: {}.", deadlineManager.getClass().getSimpleName());

            configurer.registerComponent(DeadlineManager.class, c -> deadlineManager);
        }

        if (this.eventStorageEngineProducer != null) {
            final EventStorageEngine eventStorageEngine = produce(beanManager, eventStorageEngineProducer);

            logger.info("Registering event storage: {}.", eventStorageEngine.getClass().getSimpleName());

            configurer.configureEmbeddedEventStore(c -> eventStorageEngine);
        }
        return configurer;
    }

    private <T> void addIfNotConfigured(Class<T> componentType, Producer<T> componentProducer,
                                        Supplier<T> componentSupplier, AfterBeanDiscovery afterBeanDiscovery) {
        if (componentProducer == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(componentType, componentSupplier));
        }
    }

    public void addUnconfiguredProduces(AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        // Mark: I tried removing some of this code in favor of globally enabled alternatives.
        // These are in AxonDefaultConfiguration. Howver, I immidiately ran into
        // circular reference issues. I do not think alternatives are really
        // a viable solution for this use case and I need to keep sticking to something
        // similar to this unless you know of a way.
        addIfNotConfigured(CommandBus.class, commandBusProducer,
                () -> CdiUtilities.getReference(beanManager, Configuration.class).commandBus(),
                afterBeanDiscovery);
        addIfNotConfigured(QueryBus.class, queryBusProducer,
                () -> CdiUtilities.getReference(beanManager, Configuration.class).queryBus(),
                afterBeanDiscovery);
        addIfNotConfigured(QueryGateway.class, queryGatewayProducer,
                () -> CdiUtilities.getReference(beanManager, Configuration.class).queryGateway(),
                afterBeanDiscovery);
        addIfNotConfigured(QueryUpdateEmitter.class,
                queryUpdateEmitterProducer,
                () -> CdiUtilities.getReference(beanManager, Configuration.class).queryUpdateEmitter(),
                afterBeanDiscovery);
        addIfNotConfigured(EventBus.class, eventBusProducer,
                () -> CdiUtilities.getReference(beanManager, Configuration.class).eventBus(),
                afterBeanDiscovery);
        addIfNotConfigured(Serializer.class, serializerProducer,
                () -> CdiUtilities.getReference(beanManager, Configuration.class).serializer(),
                afterBeanDiscovery);

    }

    public void setEventStorageEngineProducer(Producer<EventStorageEngine> eventStorageEngineProducer) {
        this.eventStorageEngineProducer = eventStorageEngineProducer;
    }

    public void setSerializerProducer(Producer<Serializer> serializerProducer) {
        this.serializerProducer = serializerProducer;
    }

    public void setEventSerializerProducer(Producer<Serializer> eventSerializerProducer) {
        this.eventSerializerProducer = eventSerializerProducer;
    }

    public void setMessageSerializerProducer(Producer<Serializer> messageSerializerProducer) {
        this.messageSerializerProducer = messageSerializerProducer;
    }

    public void setEventBusProducer(Producer<EventBus> eventBusProducer) {
        this.eventBusProducer = eventBusProducer;
    }

    public void setCommandBusProducer(Producer<CommandBus> commandBusProducer) {
        this.commandBusProducer = commandBusProducer;
    }

    public void setConfigurerProducer(Producer<Configurer> configurerProducer) {
        this.configurerProducer = configurerProducer;
    }

    public void setTransactionManagerProducer(Producer<TransactionManager> transactionManagerProducer) {
        this.transactionManagerProducer = transactionManagerProducer;
    }

    public void setEntityManagerProviderProducer(Producer<EntityManagerProvider> entityManagerProviderProducer) {
        this.entityManagerProviderProducer = entityManagerProviderProducer;
    }

    public void setTokenStoreProducer(Producer<TokenStore> tokenStoreProducer) {
        this.tokenStoreProducer = tokenStoreProducer;
    }

    public void setListenerInvocationErrorHandlerProducer(Producer<ListenerInvocationErrorHandler> listenerInvocationErrorHandlerProducer) {
        this.listenerInvocationErrorHandlerProducer = listenerInvocationErrorHandlerProducer;
    }

    public void setErrorHandlerProducer(Producer<ErrorHandler> errorHandlerProducer) {
        this.errorHandlerProducer = errorHandlerProducer;
    }

    public void setQueryBusProducer(Producer<QueryBus> queryBusProducer) {
        this.queryBusProducer = queryBusProducer;
    }

    public void setQueryGatewayProducer(Producer<QueryGateway> queryGatewayProducer) {
        this.queryGatewayProducer = queryGatewayProducer;
    }

    public void setQueryUpdateEmitterProducer(Producer<QueryUpdateEmitter> queryUpdateEmitterProducer) {
        this.queryUpdateEmitterProducer = queryUpdateEmitterProducer;
    }

    public void setDeadlineManagerProducer(Producer<DeadlineManager> deadlineManagerProducer) {
        this.deadlineManagerProducer = deadlineManagerProducer;
    }

    public void setEventHandlingConfigurationProducer(Producer<EventHandlingConfiguration> eventHandlingConfigurationProducer) {
        this.eventHandlingConfigurationProducer = eventHandlingConfigurationProducer;
    }

    public void setEventProcessingConfigurationProducer(Producer<EventProcessingConfiguration> eventProcessingConfigurationProducer) {
        this.eventProcessingConfigurationProducer = eventProcessingConfigurationProducer;
    }

    public void addCorrelationDataProviderProducers(Producer<CorrelationDataProvider> producer) {
        this.correlationDataProviderProducers.add(producer);
    }

    public void addModuleConfigurationProducer(Producer<ModuleConfiguration> producer) {
        this.moduleConfigurationProducers.add(producer);
    }

    public void addConfigurerModuleProducers(Producer<ConfigurerModule> producer) {
        this.configurerModuleProducers.add(producer);
    }

    public void addEventUpcasterProducers(Producer<EventUpcaster> producer) {
        this.eventUpcasterProducers.add(producer);
    }

    public EventHandlingConfiguration getEventHandlingConfiguration() {
        return this.eventHandlingConfiguration;
    }
}
