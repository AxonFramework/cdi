package org.axonframework.cdi.config;

import org.axonframework.cdi.BeanWrapper;
import org.axonframework.cdi.SagaDefinition;
import org.axonframework.config.Configurer;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.saga.repository.SagaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.axonframework.cdi.AxonCdiExtension.produce;


public class SagaConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final List<SagaDefinition> sagas = new ArrayList<>();
    private final Map<String, Producer<SagaStore>> sagaStoreProducerMap = new HashMap<>();
    private final Map<String, Producer<SagaConfiguration<?>>> sagaConfigurationProducerMap = new HashMap<>();

    public void addSaga(Class<?> sagaClass) {
        sagas.add(new SagaDefinition(sagaClass));
    }

    public void addSagaStoreProducer(String sagaStoreName, Producer<SagaStore> producer) {
        this.sagaStoreProducerMap.put(sagaStoreName, producer);
    }

    public void addSagaConfigurationProducer(String sagaConfigurationName, Producer<SagaConfiguration<?>> producer) {
        sagaConfigurationProducerMap.put(sagaConfigurationName, producer);
    }

    public void registerSagaStore(BeanManager beanManager, Configurer configurer) {
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
    public void registerSagas(BeanManager beanManager, AfterBeanDiscovery afterBeanDiscovery, Configurer configurer) {
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
}
