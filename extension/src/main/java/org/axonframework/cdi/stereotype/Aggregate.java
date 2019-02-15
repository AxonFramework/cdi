package org.axonframework.cdi.stereotype;

import org.axonframework.modelling.command.AggregateRoot;

import javax.enterprise.inject.Stereotype;
import javax.inject.Named;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that informs Axon that a given {@link Named} is an aggregate
 * instance.
 *
 * @author Simon Zambrovski, Holisticon AG
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Stereotype
@AggregateRoot
public @interface Aggregate {

    /**
     * Selects the name of the AggregateRepository bean. If left empty a new 
     * repository is created. In that case the name of the repository will be 
     * based on the simple name of the aggregate's class.
     */
    String repository() default "";

    /**
     * Sets the name of the bean providing the snapshot trigger definition. 
     * If none is provided, no snapshots are created, unless explicitly 
     * configured on the referenced repository.
     * <p>
     * Note that the use of {@link #repository()} overrides this setting, as a 
     * repository explicitly defines the snapshot trigger definition.
     */
    String snapshotTriggerDefinition() default "";

    /**
     * Get the String representation of the aggregate's type. Optional. 
     * This defaults to the simple name of the annotated class.
     */
    // TODO: 8/29/2018 it looks like this field is not used in the 
    // spring module, check why.
    String type() default "";

    /**
     * Selects the name of the {@link org.axonframework.commandhandling.CommandTargetResolver} bean. 
     * If left empty, {@link org.axonframework.commandhandling.CommandTargetResolver} 
     * bean from the application context will be used. If the bean is not 
     * defined in the application context, {@link org.axonframework.commandhandling.AnnotationCommandTargetResolver}
     * will be used.
     */
    String commandTargetResolver() default "";
}