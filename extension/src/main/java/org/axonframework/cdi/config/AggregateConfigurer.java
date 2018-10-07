package org.axonframework.cdi.config;

import org.axonframework.cdi.AggregateDefinition;
import org.axonframework.commandhandling.CommandTargetResolver;
import org.axonframework.commandhandling.model.GenericJpaRepository;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.lock.LockFactory;
import org.axonframework.common.lock.NullLockFactory;
import org.axonframework.config.Configurer;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.axonframework.cdi.AxonCdiExtension.produce;


public class AggregateConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final Map<String, Producer<Repository>> aggregateRepositoryProducerMap = new HashMap<>();
    private final Map<String, Producer<SnapshotTriggerDefinition>> snapshotTriggerDefinitionProducerMap = new HashMap<>();
    private final Map<String, Producer<CommandTargetResolver>> commandTargetResolverProducerMap = new HashMap<>();

    private final List<AggregateDefinition> aggregates = new ArrayList<>();

    public void addAggregate(Class<?> aggregateClass) {
        aggregates.add(new AggregateDefinition(aggregateClass));
    }

    public void addRepositoryProducer(final String repositoryName, Producer<Repository> producer) {
        this.aggregateRepositoryProducerMap.put(repositoryName, producer);
    }

    public void addSnapshotTriggerDefinitionProducer(final String triggerDefinitionName, Producer<SnapshotTriggerDefinition> producer) {
        this.snapshotTriggerDefinitionProducerMap.put(triggerDefinitionName, producer);
    }

    public void addCommandTargetResolverProducer(final String resolverName, Producer<CommandTargetResolver> producer) {
        this.commandTargetResolverProducerMap.put(resolverName, producer);
    }

    @SuppressWarnings("unchecked")
    public void registerAggregates(BeanManager beanManager, Configurer configurer) {
        aggregates.forEach(aggregateDefinition -> {
            logger.info("Registering aggregate: {}.", aggregateDefinition.aggregateType().getSimpleName());

            org.axonframework.config.AggregateConfigurer<?> aggregateConfigurer
                    = org.axonframework.config.AggregateConfigurer.defaultConfiguration(aggregateDefinition.aggregateType());

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

}
