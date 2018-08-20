package org.axonframework.cdi.eventhandling;

import java.util.List;
import java.util.function.Consumer;
import org.axonframework.eventhandling.EventMessage;

/**
 * Interface hiding wildcard types in order to be produced by CDI.
 *
 * @author Simon Zambrovski
 */
// Antoine: This exists only to be able to scan the producer which can 
// contain generics. See if there is a way around creating this wrapper 
// API? Is this a bug in the CDI specification? A result of type erasure 
// for reflection?
public interface MessageConsumer extends Consumer<List<? extends EventMessage<?>>> {
}
