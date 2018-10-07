package org.axonframework.cdi;

import org.axonframework.cdi.stereotype.Saga;

import java.util.Optional;

/**
 * @author Milan Savic
 */
public class SagaDefinition {

    private final Class<?> sagaType;

    public SagaDefinition(Class<?> sagaType) {
        this.sagaType = sagaType;
    }

    public Class<?> sagaType() {
        return sagaType;
    }

    public Optional<String> sagaStore() {
        return StringUtilities.createOptional(getSagaAnnotation().sagaStore());
    }

    public Optional<String> configurationBean() {
        return StringUtilities.createOptional(getSagaAnnotation().configurationBean());
    }

    public boolean explicitConfiguration() {
        return configurationBean().isPresent();
    }

    public String configurationName() {
        return configurationBean().orElse(StringUtilities.lowerCaseFirstLetter(
                sagaType().getSimpleName()) + "Configuration");
    }

    private Saga getSagaAnnotation() {
        return sagaType.getAnnotation(Saga.class);
    }
}
