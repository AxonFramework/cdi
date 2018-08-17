package org.axonframework.cdi.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.inject.Named;

/**
 * Annotation that informs Axon that a given {@link Named} is an subscribing
 * event processor.
 */
// TODO Check if there is a Spring equivalent to this.
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribingEventProcessor {

    @Nonbinding
    String packageName();
}
