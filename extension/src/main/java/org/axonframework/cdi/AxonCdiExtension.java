package org.axonframework.cdi;

import org.axonframework.cdi.config.AggregateConfigurer;
import org.axonframework.cdi.config.AxonCdiConfigurer;
import org.axonframework.cdi.config.BeanConfigurer;
import org.axonframework.cdi.config.SagaConfigurer;
import org.axonframework.cdi.stereotype.Aggregate;
import org.axonframework.cdi.stereotype.Saga;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandTargetResolver;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.config.*;
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.inject.Named;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Main CDI extension class responsible for collecting CDI beans and setting up
 * Axon configuration.
 */
public class AxonCdiExtension implements Extension {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private AggregateConfigurer aggregates = new AggregateConfigurer();
    private SagaConfigurer sagas = new SagaConfigurer();
    private AxonCdiConfigurer axonConfigurer = new AxonCdiConfigurer();
    private BeanConfigurer beans = new BeanConfigurer();

    /*
     * Saga related Event handlers
     */

    <T> void processSaga(@Observes @WithAnnotations({Saga.class})
                         final ProcessAnnotatedType<T> processAnnotatedType) {
        // TODO Saga classes may need to be vetoed so that CDI does not
        // actually try to manage them.

        final Class<?> clazz = processAnnotatedType.getAnnotatedType().getJavaClass();

        logger.debug("Found saga: {}.", clazz);

        sagas.addSaga(clazz);
    }

    <T> void processSagaConfigurationProducer(
            @Observes final ProcessProducer<T, SagaConfiguration<?>> processProducer) {
        logger.debug("Producer for saga configuration found: {}.",
                processProducer.getProducer());

        final String sagaConfigurationName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        sagas.addSagaConfigurationProducer(sagaConfigurationName, processProducer.getProducer());
    }

    <T> void processSagaStoreProducer(
            @Observes final ProcessProducer<T, SagaStore> processProducer) {
        logger.debug("Producer for saga store found: {}.", processProducer.getProducer());

        final String sagaStoreName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        sagas.addSagaStoreProducer(sagaStoreName, processProducer.getProducer());
    }

    /*
     * Aggregate related Event handlers
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

        aggregates.addAggregate(clazz);
    }

    // Mark: Same issues with the stuff below. A bit ugly but functional. I may
    // be able to get rid of most fo this by doing BeanManager look ups right
    // after afterBeanDiscovery or later.
    <T> void processAggregateRepositoryProducer(
            @Observes final ProcessProducer<T, Repository> processProducer) {
        logger.debug("Found producer for repository: {}.", processProducer.getProducer());

        final String repositoryName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        aggregates.addRepositoryProducer(repositoryName, processProducer.getProducer());
    }

    <T> void processSnapshotTriggerDefinitionProducer(
            @Observes final ProcessProducer<T, SnapshotTriggerDefinition> processProducer) {
        logger.debug("Found producer for snapshot trigger definition: {}.",
                processProducer.getProducer());

        final String triggerDefinitionName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        aggregates.addSnapshotTriggerDefinitionProducer(triggerDefinitionName, processProducer.getProducer());
    }

    <T> void processCommandTargetResolverProducer(
            @Observes final ProcessProducer<T, CommandTargetResolver> processProducer) {
        logger.debug("Found producer for command target resolver: {}.", processProducer.getProducer());

        final String resolverName = CdiUtilities.extractBeanName(processProducer.getAnnotatedMember());

        aggregates.addCommandTargetResolverProducer(resolverName, processProducer.getProducer());
    }

    /*
     * Bean (MessageHandler) related Event handlers
     */

    /**
     * Scans all beans and collects beans with message handlers.
     *
     * @param processBean bean processing event.
     */
    // Mark: This one is especally tricky. I am looking for the existance
    // of annotations and methods and collecting bean definitions.
    // I suspect this is the most efficient way to do this and I can use
    // the bean defenitions to look up via BeanManager later.
    public <T> void processBean(@Observes final ProcessBean<T> processBean) {
        final Class<?> clazz = processBean.getBean().getBeanClass();
        if (clazz.getName().startsWith("io.axoniq.") || clazz.getName().startsWith("org.axonframework.")) {
            logger.info("ProcessInjectionTarget<{}>", clazz.getName());
        }
        MessageHandlingBeanDefinition.inspect(processBean.getBean())
                .ifPresent(bean -> {
                    logger.info("Found {}.", bean);
                    beans.addMessageHandler(bean.getBean().getBeanClass(), bean);
                });
    }

    public <T> void processInjectionTarget(@Observes final ProcessInjectionTarget<T> injectionTarget)
            throws IOException
    {
        final Class<T> clazz = injectionTarget.getAnnotatedType().getJavaClass();
//        if (clazz.getName().startsWith("io.axoniq.") || clazz.getName().startsWith("org.axonframework.")) {
            logger.info("ProcessInjectionTarget<{}>", clazz.getName());
//        }
        if (beans.isMessageHandler(clazz)) {
            logger.info("ProcessInjectionTarget<{}>: Found a MessageHandler", clazz.getName());
        }
    }

    /*
     * Event handlers for producers of the different beans we need to configure Axon.
     */

    /**
     * Scans for an event storage engine producer.
     *
     * @param processProducer process producer event.
     */
    // Mark: I know these seem especially frivolous and looks like they may be
    // replaced with post deployment look-ups or injection of some kind.
    // Unfortunately I don't think it is that straightforwards for reasons
    // explained a bit later. That said, I do want to discuss these with you.
    <T> void processEventStorageEngineProducer(
            @Observes final ProcessProducer<T, EventStorageEngine> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for event storage engine found: {}.",
                processProducer.getProducer());

        axonConfigurer.setEventStorageEngineProducer(processProducer.getProducer());
    }

    /**
     * Scans for a configurer producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processConfigurerProducer(
            @Observes final ProcessProducer<T, Configurer> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for configurer found: {}.", processProducer.getProducer());

        axonConfigurer.setConfigurerProducer(processProducer.getProducer());
    }

    /**
     * Scans for a transaction manager producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processTransactionManagerProducer(
            @Observes final ProcessProducer<T, TransactionManager> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for transaction manager found: {}.",
                processProducer.getProducer());

        axonConfigurer.setTransactionManagerProducer(processProducer.getProducer());
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
                    logger.debug("Producer for event serializer found: {}.",
                            processProducer.getProducer());
                    axonConfigurer.setEventSerializerProducer(processProducer.getProducer());
                    break;
                case "messageSerializer":
                    logger.debug("Producer for message serializer found: {}.",
                            processProducer.getProducer());
                    axonConfigurer.setMessageSerializerProducer(processProducer.getProducer());
                    break;
                case "serializer":
                    logger.debug("Producer for serializer found: {}.",
                            processProducer.getProducer());
                    axonConfigurer.setSerializerProducer(processProducer.getProducer());
                    break;
                default:
                    logger.warn("Unknown named serializer configured: " + serializerName);
            }
        } else {
            logger.debug("Producer for serializer found: {}.", processProducer.getProducer());
            axonConfigurer.setSerializerProducer(processProducer.getProducer());
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

        logger.debug("Producer for event bus found: {}.", processProducer.getProducer());

        axonConfigurer.setEventBusProducer(processProducer.getProducer());
    }

    /**
     * Scans for a command bus producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processCommandBusProducer(
            @Observes final ProcessProducer<T, CommandBus> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for command bus found: {}.",
                processProducer.getProducer());

        axonConfigurer.setCommandBusProducer(processProducer.getProducer());
    }

//    <T> void processCommandGatewayProducer(
//            @Observes final ProcessProducer<T, CommandGateway> processProducer) {
//        // TODO Handle multiple producer definitions.
//
//        logger.debug("Producer for command gateway found: {}.",
//                processProducer.getProducer());
//
//        axonConfigurer.setCommandGatewayProducer(processProducer.getProducer());
//    }

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

        logger.debug("Producer for entity manager provider found: {}.",
                processProducer.getProducer());

        axonConfigurer.setEntityManagerProviderProducer(processProducer.getProducer());
    }

    /**
     * Scans for a token store producer.
     *
     * @param processProducer process producer event.
     */
    <T> void processTokenStoreProducer(
            @Observes final ProcessProducer<T, TokenStore> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for token store found: {}.", processProducer.getProducer());

        axonConfigurer.setTokenStoreProducer(processProducer.getProducer());
    }

    <T> void processErrorHandlerProducer(
            @Observes final ProcessProducer<T, ErrorHandler> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for error handler found: {}.", processProducer.getProducer());

        axonConfigurer.setErrorHandlerProducer(processProducer.getProducer());
    }

    <T> void processListenerInvocationErrorHandlerProducer(
            @Observes final ProcessProducer<T, ListenerInvocationErrorHandler> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for listener invocation error handler found: {}.",
                processProducer.getProducer());

        axonConfigurer.setListenerInvocationErrorHandlerProducer(processProducer.getProducer());
    }

    <T> void processCorrelationDataProviderProducer(
            @Observes final ProcessProducer<T, CorrelationDataProvider> processProducer) {
        logger.debug("Producer for correlation data provider found: {}.",
                processProducer.getProducer());

        axonConfigurer.addCorrelationDataProviderProducers(processProducer.getProducer());
    }

    <T> void processQueryBusProducer(
            @Observes final ProcessProducer<T, QueryBus> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for query bus found: {}.",
                processProducer.getProducer());

        axonConfigurer.setQueryBusProducer(processProducer.getProducer());
    }

    <T> void processQueryGatewayProducer(
            @Observes final ProcessProducer<T, QueryGateway> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for query gateway found: {}.",
                processProducer.getProducer());

        axonConfigurer.setQueryGatewayProducer(processProducer.getProducer());
    }

    <T> void processQueryUpdateEmitterProducer(
            @Observes final ProcessProducer<T, QueryUpdateEmitter> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for query update emitter found: {}.", processProducer.getProducer());

        axonConfigurer.setQueryUpdateEmitterProducer(processProducer.getProducer());
    }

    <T> void processDeadlineManagerProducer(
            @Observes final ProcessProducer<T, DeadlineManager> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for deadline manager found: {}.", processProducer.getProducer());

        axonConfigurer.setDeadlineManagerProducer(processProducer.getProducer());
    }

    <T> void processModuleConfigurationProducer(
            @Observes final ProcessProducer<T, ModuleConfiguration> processProducer) {
        logger.debug("Producer for module configuration found: {}.",
                processProducer.getProducer());

        axonConfigurer.addModuleConfigurationProducer(processProducer.getProducer());
    }

    <T> void processEventHandlingConfigurationProducer(
            @Observes final ProcessProducer<T, EventHandlingConfiguration> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for event handling configuration found: {}.",
                processProducer.getProducer());

        axonConfigurer.setEventHandlingConfigurationProducer(processProducer.getProducer());
    }

    <T> void processEventProcessingConfigurationProducer(
            @Observes final ProcessProducer<T, EventProcessingConfiguration> processProducer) {
        // TODO Handle multiple producer definitions.

        logger.debug("Producer for event processing configuration found: {}.",
                processProducer.getProducer());

        axonConfigurer.setEventProcessingConfigurationProducer(processProducer.getProducer());
    }

    <T> void processConfigurerModuleProducer(
            @Observes final ProcessProducer<T, ConfigurerModule> processProducer) {
        logger.debug("Producer for configurer module found: {}.", processProducer.getProducer());

        axonConfigurer.addConfigurerModuleProducers(processProducer.getProducer());
    }

    <T> void processEventUpcasterProducer(
            @Observes final ProcessProducer<T, EventUpcaster> processProducer) {
        logger.debug("Producer for event upcaster found: {}.", processProducer.getProducer());

        axonConfigurer.addEventUpcasterProducers(processProducer.getProducer());
    }

    // Mark: Many of the beans and producers I am processing may use
    // container resources such as entity managers, etc. I believe this means
    // I should be handling them as late as possible to avoid initialization
    // timing issues. Right now things are processed as they make
    // "semantic" sense. Do you think this could be improved to do the same
    // processing later?

    /**
     * Registration of Axon components in CDI registry.
     */
    void afterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery,
            final BeanManager beanManager) {
        logger.info("Starting Axon Framework configuration.");

        Configurer configurer = axonConfigurer.produceBeans(beanManager);
        // Now need to begin registering application components rather than
        // configuration components.
        aggregates.registerAggregates(beanManager, configurer);
        sagas.registerSagaStore(beanManager, configurer);
        sagas.registerSagas(beanManager, afterBeanDiscovery, configurer);
        beans.registerMessageHandlers(beanManager, configurer, axonConfigurer.getEventHandlingConfiguration());

        logger.info("Axon Framework configuration complete.");

        logger.info("Registering Axon APIs with CDI.");

        
        // Mark: This is one of the key parts that is forcing me to do anything at all
        // in the afterBeanDiscovery phase. Once the Axon configuration completes,
        // including taking into account user defined overrides, I need to bind some
        // bean definitions with CDI. Aside from these relatively static definitions,
        // there are and will be loops where I need to register things that look like
        // Repository<AggregateType1>...Repository<AggregateTypeN>. I think this means
        // I absolutely need to make sure of and thus finish my Axon configuration somehow very
        // close to afterBeanDiscovery, in a programmatic fashion.
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
                new BeanWrapper<>(Configuration.class, () -> startConfiguration(configurer)));

        axonConfigurer.addUnconfiguredProduces(afterBeanDiscovery, beanManager);
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

    private Configuration startConfiguration(Configurer configurer) {
        logger.info("Starting Axon configuration.");

        return configurer.start();
    }

    public static  <T> T produce(BeanManager beanManager, Producer<T> producer) {
        return producer.produce(beanManager.createCreationalContext(null));
    }
}
