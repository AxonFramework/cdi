package org.axonframework.cdi.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.inject.Stereotype;

/**
 * @author Milan Savic
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Stereotype
public @interface Saga {

    /**
     * Selects the name of the SagaStore bean. If left empty the saga will be stored in the Saga Store configured in the
     * global Axon Configuration.
     */
    String sagaStore() default "";

    /**
     * Defines the name of the bean that configures this Saga type. When defined, a bean of type {@link
     * org.axonframework.config.SagaConfiguration} with such name must exist. If not defined, Axon will attempt to
     * locate a bean named `&lt;sagaSimpleClassName&gt;Configuration`, creating a default configuration if none is
     * found.
     */
    String configurationBean() default "";
}
