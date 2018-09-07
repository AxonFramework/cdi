package org.axonframework.cdi;

import org.axonframework.cdi.stereotype.Saga;

import java.util.Optional;

/**
 * @author Milan Savic
 */
class SagaDefinition {

    private final Class<?> sagaType;

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

    private Saga getSagaAnnotation() {
        return sagaType.getAnnotation(Saga.class);
    }
}
