package org.axonframework.cdi.messaging;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.SubscribableMessageSource;

/**
 * Interface hiding wildcard types in order to be produced by CDI.
 *
 * @author Simon Zambrovski
 */
// TODO See if there is a way around creating this wrapper API. Is this a bug
// in the CDI specification? A result of type erasure for reflection?
public interface SubscribableEventMessageSource 
        extends SubscribableMessageSource<EventMessage<?>> {
}