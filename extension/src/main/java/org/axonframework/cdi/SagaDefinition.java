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
        return createOptional(getSagaAnnotation().sagaStore());
    }

    Optional<String> configurationBean() {
        return createOptional(getSagaAnnotation().configurationBean());
    }

    boolean explicitConfiguration() {
        return configurationBean().isPresent();
    }

    String configurationName() {
        return configurationBean().orElse(lcFirst(sagaType().getSimpleName()) + "Configuration");
    }

    private Saga getSagaAnnotation() {
        return sagaType.getAnnotation(Saga.class);
    }

    private String lcFirst(String string) {
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    private Optional<String> createOptional(String value) {
        if ("".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
