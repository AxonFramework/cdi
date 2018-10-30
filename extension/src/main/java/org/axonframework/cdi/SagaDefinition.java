package org.axonframework.cdi;

import org.axonframework.cdi.stereotype.Saga;

import java.util.Optional;
import org.axonframework.config.SagaConfiguration;

/**
 * @author Milan Savic
 */
class SagaDefinition {

    private final Class<?> sagaType;
    private SagaConfiguration<?> sagaConfiguration;

    SagaDefinition(Class<?> sagaType) {
        this.sagaType = sagaType;
    }

    Class<?> sagaType() {
        return sagaType;
    }

    Optional<String> sagaStore() {
        return StringUtilities.createOptional(getSagaAnnotation().sagaStore());
    }

    Optional<String> configurationBean() {
        return StringUtilities.createOptional(getSagaAnnotation().configurationBean());
    }

    boolean explicitConfiguration() {
        return configurationBean().isPresent();
    }

    String configurationName() {
        return configurationBean().orElse(StringUtilities.lowerCaseFirstLetter(
                sagaType().getSimpleName()) + "Configuration");
    }

    void setSagaConfiguration(SagaConfiguration<?> sagaConfiguration) {
        this.sagaConfiguration = sagaConfiguration;
    }

    SagaConfiguration<?> getSagaConfiguration() {
        return sagaConfiguration;
    }

    private Saga getSagaAnnotation() {
        return sagaType.getAnnotation(Saga.class);
    }
}
