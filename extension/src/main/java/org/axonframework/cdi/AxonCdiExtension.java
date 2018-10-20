package org.axonframework.cdi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.WithAnnotations;
import org.axonframework.cdi.stereotype.Aggregate;
import org.axonframework.cdi.stereotype.Saga;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandTargetResolver;
import org.axonframework.commandhandling.model.GenericJpaRepository;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.lock.LockFactory;
import org.axonframework.common.lock.NullLockFactory;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.AggregateConfigurer;
import org.axonframework.config.Component;
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

    private Bean commandBusBean;
    private Bean queryBusBean;
    private Bean queryGatewayBean;
    private Bean queryUpdateEmitterBean;
    private Bean eventBusBean;
    private Bean serializerBean;
    private Bean eventSerializerBean;
    private Bean messageSerializerBean;

    private final List<AggregateDefinition> aggregates = new ArrayList<>();
    private final Map<String, Producer<Repository>> aggregateRepositoryProducerMap = new HashMap<>();
    private final Map<String, Producer<SnapshotTriggerDefinition>> snapshotTriggerDefinitionProducerMap = new HashMap<>();
    private final Map<String, Producer<CommandTargetResolver>> commandTargetResolverProducerMap = new HashMap<>();

    private final List<SagaDefinition> sagas = new ArrayList<>();
    private final Map<String, Producer<SagaStore>> sagaStoreProducerMap = new HashMap<>();
    private final Map<String, Producer<SagaConfiguration<?>>> sagaConfigurationProducerMap = new HashMap<>();

    private final List<MessageHandlingBeanDefinition> messageHandlers = new ArrayList<>();

    // Mark: Many of the beans and producers I am processing may use
    // container resources such as entity managers, etc. I believe this means
    // I should be handling them as late as possible to avoid initialization
    // timing issues. Right now things are processed as they make
    // "semantic" sense. Do you think this could be improved to do the same
    // processing later?
    //X @struberg: it will work. But probably might be better to observe ProcessBeanAttributes pba and use pba.getAnnotated
    //X that way you will also have the ability to pick up modifications of those classes via ProcessAnnotatedType.
    //X Not sure if this is really needed for your users though.
    /**
     * Scans all annotated types with the {@link Aggregate} annotation and
     * collects them for registration.
     *
     * @param processAnnotatedType annotated type processing event.
     */
    // Mark: All I need to do here is look up what the aggregate classes are
    // and what the value of the @Aggregate annotation is. This feels a little
    // overkill. That said, currently I do need these values in afterBeanDiscovery
    // and this might be the most efficient way of collecting these anyway.
    // Other than being a bit ugly, this is not anything that is causing any
    // functional issues.
    <T> void processAggregate(@Observes @WithAnnotations({Aggregate.class})
            final ProcessAnnotatedType<T> processAnnotatedType) {
        // TODO Aggregate classes may need to be vetoed so that CDI does not
        // actually try to manage them.

        final Class<?> clazz = processAnnotatedType.getAnnotatedType().getJavaClass();

        logger.debug("Found aggregate: {}.", clazz);

        aggregates.add(new AggregateDefinition(clazz));
    }

    // Mark: Same issues with the stuff below. A bit ugly but functional. I may
    // be able to get rid of most fo this by doing BeanManager look ups right
    // after afterBeanDiscovery or later.
    <T> void processAggregateRepositoryProducer(
            @Observes final ProcessProducer<T, Repository> processProducer) {
        logger.debug("Found producer for repository: {}.", processProducer.getProducer());

        String repositoryName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.aggregateRepositoryProducerMap.put(repositoryName, processProducer.getProducer());
    }

    <T> void processSnapshotTriggerDefinitionProducer(
            @Observes final ProcessProducer<T, SnapshotTriggerDefinition> processProducer) {
        logger.debug("Found producer for snapshot trigger definition: {}.",
                processProducer.getProducer());

        String triggerDefinitionName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.snapshotTriggerDefinitionProducerMap.put(triggerDefinitionName, processProducer.getProducer());
    }

    <T> void processCommandTargetResolverProducer(
            @Observes final ProcessProducer<T, CommandTargetResolver> processProducer) {
        logger.debug("Found producer for command target resolver: {}.", processProducer.getProducer());

        String resolverName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.commandTargetResolverProducerMap.put(resolverName, processProducer.getProducer());
    }

    <T> void processSaga(@Observes @WithAnnotations({Saga.class})
            final ProcessAnnotatedType<T> processAnnotatedType) {
        // TODO Saga classes may need to be vetoed so that CDI does not
        // actually try to manage them.

        final Class<?> clazz = processAnnotatedType.getAnnotatedType().getJavaClass();

        logger.debug("Found saga: {}.", clazz);

        sagas.add(new SagaDefinition(clazz));
    }

    /**
     * Scans for an event storage engine producer.
     *
     * @param processProducer process producer event.
     */
    // Mark: I know these seem especially frivolous and looks like they may be
    // replaced with post deployment look-ups or injection of some kind.
    // Unfortunately I don't think it is that straightforwards for reasons
    // explained a bit later. That said, I do want to discuss these with you.
    <T> void processSagaConfigurationProducer(
            @Observes final ProcessProducer<T, SagaConfiguration<?>> processProducer) {
        logger.debug("Producer for saga configuration found: {}.",
                processProducer.getProducer());

        String sagaConfigurationName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        sagaConfigurationProducerMap.put(sagaConfigurationName, processProducer.getProducer());
    }

    <T> void processSagaStoreProducer(
            @Observes final ProcessProducer<T, SagaStore> processProducer) {
        logger.debug("Producer for saga store found: {}.", processProducer.getProducer());

        String sagaStoreName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        this.sagaStoreProducerMap.put(sagaStoreName, processProducer.getProducer());
    }

    /**
     * Scans all beans and collects beans with message handlers.
     *
     * @param processBean bean processing event.
     */
    // Mark: This one is especally tricky. I am looking for the existance
    // of annotations and methods and collecting bean definitions.
    // I suspect this is the most efficient way to do this and I can use
    // the bean defenitions to look up via BeanManager later.
    //X @struberg it's fine. Although I would use processBean.getAnnotated() instead of bean.getBeanClass() in the 'inspect' code
    //X I'fe quickly fixed this method. Please verify if it works as expected. And you still need to change the tests to AnnotatedType!
    <T> void processBean(@Observes final ProcessBean<T> processBean) {
        MessageHandlingBeanDefinition.inspect(processBean.getBean(), processBean.getAnnotated())
                .ifPresent(bean -> {
                    logger.debug("Found {}.", bean);
                    messageHandlers.add(bean);
                });
    }

    /**
     * Registration of Axon components in CDI registry.
     */
    void afterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery,
            final BeanManager beanManager) {
        logger.info("Registering Axon APIs with CDI.");

        detectOverrides(beanManager);

        // Mark: This is one of the key parts that is forcing me to do anything at all
        // in the afterBeanDiscovery phase. Once the Axon configuration completes,
        // including taking into account user defined overrides, I need to bind some
        // bean definitions with CDI. Aside from these relatively static definitions,
        // there are and will be loops where I need to register things that look like
        // Repository<AggregateType1>...Repository<AggregateTypeN>. I think this means
        // I absolutely need to make sure of and thus finish my Axon configuration somehow very
        // close to afterBeanDiscovery, in a programmatic fashion.
        //X @struberg: do you know that you can send CDI events between CDI Extensions already during bootstrap?
        //X That might help here.
        //X So you effectively can send out a ConfigurationEvent and anybody could write an own CDI Extension where
        //X this gets observed. That way you can effectively have configuration and inter-CDI communication already before
        //X the CDI container is booted.
        //X I think I need a better explanation what problem you want to solve by it though.
        //
        // That said, you'll notice the lambdas below that are my effort to defer
        // actual instantiation to as late as possible. The only other way I can
        // think of to defer actual bean referencing any further is by using
        // byte code proxies that look up bean references on each method call.
        // Can you think of a better way to further defer the actual configution start and
        // bean referencing?
        //
        // Even with this deferring, I am running into issues like EJBs not
        // instantiated and JPA registries not done yet on certain containers like
        // Payara. When I refer to application scoped beans at this stage, it seems I am gettting
        // my own copy and then the application creates another copy when the
        // application code actually bootstraps. All related to referncing beans too
        // early I believe.
        afterBeanDiscovery.addBean(
                new BeanWrapper<>(Configuration.class, () -> startConfiguration(beanManager)));
        // Mark: I tried removing some of this code in favor of globally enabled alternatives.
        // These are in AxonDefaultConfiguration. Howover, I immediately ran into
        // circular reference issues. I do not think alternatives are really
        // a viable solution for this use case and I need to keep sticking to something
        // similar to this unless you know of a way.
        if (commandBusBean == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(CommandBus.class,
                    () -> CdiUtilities.getReference(beanManager, Configuration.class).commandBus()));
        }

        if (queryBusBean == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(QueryBus.class,
                    () -> CdiUtilities.getReference(beanManager, Configuration.class).queryBus()));
        }

        if (queryGatewayBean == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(QueryGateway.class,
                    () -> CdiUtilities.getReference(beanManager, Configuration.class).queryGateway()));
        }

        if (queryUpdateEmitterBean == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(QueryUpdateEmitter.class,
                    () -> CdiUtilities.getReference(beanManager, Configuration.class).queryUpdateEmitter()));
        }

        if (eventBusBean == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(EventBus.class,
                    () -> CdiUtilities.getReference(beanManager, Configuration.class).eventBus()));
        }

        if (serializerBean == null) {
            afterBeanDiscovery.addBean(new BeanWrapper<>(Serializer.class,
                    () -> CdiUtilities.getReference(beanManager, Configuration.class).serializer()));
        }
    }

    private void detectOverrides(final BeanManager beanManager) {
        commandBusBean = CdiUtilities.resolveBean(beanManager, CommandBus.class);
        queryBusBean = CdiUtilities.resolveBean(beanManager, QueryBus.class);
        queryGatewayBean = CdiUtilities.resolveBean(beanManager, QueryGateway.class);
        queryUpdateEmitterBean = CdiUtilities.resolveBean(beanManager, QueryUpdateEmitter.class);
        eventBusBean = CdiUtilities.resolveBean(beanManager, EventBus.class);

        for (Bean bean : beanManager.getBeans(Serializer.class)) {
            String beanName = bean.getName();

            if ((beanName == null) || beanName.equals("serializer")) {
                serializerBean = bean;
            } else if (beanName.equals("eventSerializer")) {
                eventSerializerBean = bean;
            } else if (beanName.equals("messageSerializer")) {
                messageSerializerBean = bean;
            }
        }
    }

    void afterDeploymentValidation(
            @Observes final AfterDeploymentValidation afterDeploymentValidation,
            final BeanManager beanManager) {
        // Ensure the configuration is started.
        CdiUtilities.getReference(beanManager, Configuration.class).commandBus();
    }

    void init(@Observes @Initialized(ApplicationScoped.class) Object initialized) {
    }

    void destroy(@Observes @Destroyed(ApplicationScoped.class) final Object destroyed) {
    }

    void beforeShutdown(@Observes BeforeShutdown event, BeanManager beanManager) {
    }

    private Configuration startConfiguration(BeanManager beanManager) {
        logger.info("Starting Axon Framework configuration.");

        Configurer configurer;

        Bean configurerBean = CdiUtilities.resolveBean(beanManager, Configurer.class);

        if (configurerBean != null) {
            configurer = (Configurer) CdiUtilities.getReference(beanManager, configurerBean);

            logger.info("Starting with an application provided configurer: {}.",
                    configurer.getClass().getSimpleName());
        } else {
            logger.info("Starting with the Axon default configuration.");

            configurer = DefaultConfigurer.defaultConfiguration();
        }

        Bean entityManagerProviderBean = CdiUtilities.resolveBean(
                beanManager, EntityManagerProvider.class);

        if (entityManagerProviderBean != null) {
            final EntityManagerProvider entityManagerProvider
                    = (EntityManagerProvider) CdiUtilities.getReference(
                            beanManager, entityManagerProviderBean);

            logger.info("Registering entity manager provider: {}.",
                    entityManagerProvider.getClass().getSimpleName());

            configurer.registerComponent(EntityManagerProvider.class, c -> entityManagerProvider);
        }

        if (serializerBean != null) {
            Serializer serializer
                    = (Serializer) CdiUtilities.getReference(beanManager, serializerBean);
            logger.info("Registering serializer: {}.", serializer.getClass().getSimpleName());
            configurer.configureSerializer(c -> serializer);
        }

        if (eventSerializerBean != null) {
            Serializer eventSerializer
                    = (Serializer) CdiUtilities.getReference(beanManager, eventSerializerBean);
            logger.info("Registering event serializer: {}.",
                    eventSerializer.getClass().getSimpleName());
            configurer.configureEventSerializer(c -> eventSerializer);
        }

        if (messageSerializerBean != null) {
            Serializer messageSerializer
                    = (Serializer) CdiUtilities.getReference(beanManager, messageSerializerBean);
            logger.info("Registering message serializer: {}.",
                    messageSerializer.getClass().getSimpleName());
            configurer.configureMessageSerializer(c -> messageSerializer);
        }

        Bean transactionManagerBean = CdiUtilities.resolveBean(beanManager, TransactionManager.class);

        if (transactionManagerBean != null) {
            final TransactionManager transactionManager
                    = (TransactionManager) CdiUtilities.getReference(
                            beanManager, transactionManagerBean);

            logger.info("Registering transaction manager: {}.",
                    transactionManager.getClass().getSimpleName());

            configurer.configureTransactionManager(c -> transactionManager);
        }

        if (commandBusBean != null) {
            final CommandBus commandBus
                    = (CommandBus) CdiUtilities.getReference(beanManager, commandBusBean);

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
        for (Bean moduleConfigurationBean : beanManager.getBeans(ModuleConfiguration.class)) {
            configurer.registerModule(new LazyRetrievedModuleConfiguration(() -> {
                ModuleConfiguration moduleConfiguration
                        = (ModuleConfiguration) CdiUtilities.getReference(
                                beanManager, moduleConfigurationBean);
                logger.info("Registering module configuration: {}.",
                        moduleConfiguration.getClass().getSimpleName());
                return moduleConfiguration;
            }));
        }

        EventHandlingConfiguration eventHandlingConfiguration;

        Bean eventHandlingConfigurationBean = CdiUtilities.resolveBean(
                beanManager, EventHandlingConfiguration.class);

        if (eventHandlingConfigurationBean != null) {
            eventHandlingConfiguration
                    = (EventHandlingConfiguration) CdiUtilities.getReference(
                            beanManager, eventHandlingConfigurationBean);
        } else {
            eventHandlingConfiguration = new EventHandlingConfiguration();
        }

        logger.info("Registering event handling configuration: {}.",
                eventHandlingConfiguration.getClass().getSimpleName());
        configurer.registerModule(eventHandlingConfiguration);

        Bean eventProcessingConfigurationBean = CdiUtilities.resolveBean(
                beanManager, EventProcessingConfiguration.class);

        if (eventProcessingConfigurationBean != null) {
            EventProcessingConfiguration eventProcessingConfiguration
                    = (EventProcessingConfiguration) CdiUtilities.getReference(
                            beanManager, eventProcessingConfigurationBean);

            logger.info("Registering event processing configuration: {}.",
                    eventProcessingConfiguration.getClass().getSimpleName());

            configurer.registerModule(eventProcessingConfiguration);
        }

        for (Bean configurerModuleBean : beanManager.getBeans(ConfigurerModule.class)) {
            ConfigurerModule configurerModule = (ConfigurerModule) CdiUtilities.getReference(
                    beanManager, configurerModuleBean);

            logger.info("Configuring module: {}.", configurerModule.getClass().getSimpleName());

            configurerModule.configureModule(configurer);
        }

        if (eventBusBean != null) {
            final EventBus eventBus = (EventBus) CdiUtilities.getReference(
                    beanManager, eventBusBean);

            logger.info("Registering event bus: {}.", eventBus.getClass().getSimpleName());

            configurer.configureEventBus(c -> eventBus);
        }

        Bean tokenStoreBean = CdiUtilities.resolveBean(beanManager, TokenStore.class);

        if (tokenStoreBean != null) {
            final TokenStore tokenStore
                    = (TokenStore) CdiUtilities.getReference(beanManager, tokenStoreBean);

            logger.info("Registering token store: {}.", tokenStore.getClass().getSimpleName());

            configurer.registerComponent(TokenStore.class, c -> tokenStore);
        }

        Bean listenerInvocationErrorHandlerBean = CdiUtilities.resolveBean(
                beanManager, ListenerInvocationErrorHandler.class);

        if (listenerInvocationErrorHandlerBean != null) {
            ListenerInvocationErrorHandler listenerInvocationErrorHandler
                    = (ListenerInvocationErrorHandler) CdiUtilities.getReference(
                            beanManager, listenerInvocationErrorHandlerBean);

            logger.info("Registering listener invocation error handler: {}.",
                    listenerInvocationErrorHandler.getClass().getSimpleName());

            configurer.registerComponent(ListenerInvocationErrorHandler.class,
                    c -> listenerInvocationErrorHandler);
        }

        Bean errorHandlerBean = CdiUtilities.resolveBean(beanManager, ErrorHandler.class);

        if (errorHandlerBean != null) {
            ErrorHandler errorHandler = (ErrorHandler) CdiUtilities.getReference(
                    beanManager, errorHandlerBean);

            logger.info("Registering error handler: {}.", errorHandler.getClass().getSimpleName());

            configurer.registerComponent(ErrorHandler.class, c -> errorHandler);
        }

        List<CorrelationDataProvider> correlationDataProviders = new ArrayList<>();

        for (Bean correlationDataProviderBean : beanManager.getBeans(CorrelationDataProvider.class)) {
            CorrelationDataProvider correlationDataProvider
                    = (CorrelationDataProvider) CdiUtilities.getReference(
                            beanManager, correlationDataProviderBean);

            logger.info("Registering correlation data provider: {}.",
                    correlationDataProvider.getClass().getSimpleName());

            correlationDataProviders.add(correlationDataProvider);
        }

        configurer.configureCorrelationDataProviders(c -> correlationDataProviders);

        for (Bean eventUpcasterBean : beanManager.getBeans(EventUpcaster.class)) {
            EventUpcaster eventUpcaster = (EventUpcaster) CdiUtilities.getReference(
                    beanManager, eventUpcasterBean);
            logger.info("Registering event upcaster: {}.",
                    eventUpcaster.getClass().getSimpleName());
            configurer.registerEventUpcaster(c -> eventUpcaster);
        }

        if (queryBusBean != null) {
            QueryBus queryBus = (QueryBus) CdiUtilities.getReference(
                    beanManager, queryBusBean);

            logger.info("Registering query bus: {}.", queryBus.getClass().getSimpleName());

            configurer.configureQueryBus(c -> queryBus);
        }

        if (queryGatewayBean != null) {
            QueryGateway queryGateway = (QueryGateway) CdiUtilities.getReference(
                    beanManager, queryGatewayBean);

            logger.info("Registering query gateway: {}.", queryGateway.getClass().getSimpleName());

            configurer.registerComponent(QueryGateway.class, c -> queryGateway);
        }

        if (queryUpdateEmitterBean != null) {
            QueryUpdateEmitter queryUpdateEmitter
                    = (QueryUpdateEmitter) CdiUtilities.getReference(
                            beanManager, queryUpdateEmitterBean);

            logger.info("Registering query update emitter: {}.", queryUpdateEmitter.getClass().getSimpleName());

            configurer.configureQueryUpdateEmitter(c -> queryUpdateEmitter);
        }

        Bean deadlineManagerBean = CdiUtilities.resolveBean(beanManager, DeadlineManager.class);

        if (deadlineManagerBean != null) {
            DeadlineManager deadlineManager
                    = (DeadlineManager) CdiUtilities.getReference(
                            beanManager, deadlineManagerBean);

            logger.info("Registering deadline manager: {}.", deadlineManager.getClass().getSimpleName());

            configurer.registerComponent(DeadlineManager.class, c -> deadlineManager);
        }

        Bean eventStorageEngineBean = CdiUtilities.resolveBean(beanManager, EventStorageEngine.class);

        if (eventStorageEngineBean != null) {
            final EventStorageEngine eventStorageEngine
                    = (EventStorageEngine) CdiUtilities.getReference(
                            beanManager, eventStorageEngineBean);

            logger.info("Registering event storage: {}.", eventStorageEngine.getClass().getSimpleName());

            configurer.configureEmbeddedEventStore(c -> eventStorageEngine);
        }

        // Now need to begin registering application components rather than
        // configuration components.
        registerAggregates(beanManager, configurer);

        registerSagaStore(beanManager, configurer);
        registerSagas(beanManager, null, configurer);

        registerMessageHandlers(beanManager, configurer, eventHandlingConfiguration);

        logger.info("Axon Framework configuration complete.");

        logger.info("Starting Axon configuration.");

        return configurer.start();
    }

    @SuppressWarnings("unchecked")
    private void registerAggregates(BeanManager beanManager, Configurer configurer) {
        aggregates.forEach(aggregateDefinition -> {
            logger.info("Registering aggregate: {}.", aggregateDefinition.aggregateType().getSimpleName());

            AggregateConfigurer<?> aggregateConfigurer
                    = AggregateConfigurer.defaultConfiguration(aggregateDefinition.aggregateType());

            if (aggregateDefinition.repository().isPresent()) {
                aggregateConfigurer.configureRepository(
                        c -> produce(beanManager, aggregateRepositoryProducerMap
                                .get(aggregateDefinition.repository().get())));
            } else {
                if (aggregateRepositoryProducerMap.containsKey(aggregateDefinition.repositoryName())) {
                    aggregateConfigurer.configureRepository(
                            c -> produce(beanManager, aggregateRepositoryProducerMap
                                    .get(aggregateDefinition.repositoryName())));
                } else {
                    // TODO: 8/29/2018 check how to do in CDI world: register repository as a bean
                    // TODO: 8/29/2018 check how to do in CDI world: aggregate factory
                    aggregateDefinition.snapshotTriggerDefinition().ifPresent(triggerDefinition -> aggregateConfigurer
                            .configureSnapshotTrigger(
                                    c -> produce(beanManager, snapshotTriggerDefinitionProducerMap
                                            .get(triggerDefinition))));

                    if (aggregateDefinition.isJpaAggregate()) {
                        aggregateConfigurer.configureRepository(
                                c -> new GenericJpaRepository(
                                        // TODO: 8/29/2018 what to do about default EntityManagerProvider (check spring impl)
                                        c.getComponent(EntityManagerProvider.class
                                        ),
                                        aggregateDefinition.aggregateType(),
                                        c.eventBus(),
                                        c::repository,
                                        c.getComponent(LockFactory.class,
                                                () -> NullLockFactory.INSTANCE),
                                        c.parameterResolverFactory(),
                                        c.handlerDefinition(aggregateDefinition.aggregateType())));
                    }
                }
            }

            if (aggregateDefinition.commandTargetResolver().isPresent()) {
                aggregateConfigurer.configureCommandTargetResolver(
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
                        .ifPresent(resolver -> aggregateConfigurer.configureCommandTargetResolver(
                        c -> produce(beanManager,
                                commandTargetResolverProducerMap.get(resolver))));
            }

            configurer.configureAggregate(aggregateConfigurer);
        });
    }

    private void registerMessageHandlers(BeanManager beanManager, Configurer configurer,
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
                    configurer
                            .registerComponent(SagaStore.class,
                                    c -> sagaStore);
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

                afterBeanDiscovery
                        .addBean(new BeanWrapper<>(sagaDefinition.configurationName(),
                                SagaConfiguration.class,
                                () -> sagaConfiguration));
                sagaDefinition.sagaStore()
                        .ifPresent(sagaStore -> sagaConfiguration
                        .configureSagaStore(c -> produce(beanManager, sagaStoreProducerMap.get(sagaStore))));
                configurer.registerModule(sagaConfiguration);
            }
        });
    }

    private <T> T produce(BeanManager beanManager, Producer<T> producer) {
        return producer.produce(beanManager.createCreationalContext(null));
    }
}
