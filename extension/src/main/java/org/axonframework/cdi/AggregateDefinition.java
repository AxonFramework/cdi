package org.axonframework.cdi;

import org.axonframework.cdi.stereotype.Aggregate;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Milan Savic
 */
class AggregateDefinition {

    private final Class<?> aggregateType;

    AggregateDefinition(Class<?> aggregateType) {
        this.aggregateType = aggregateType;
    }

    Class<?> aggregateType() {
        return aggregateType;
    }

    Optional<String> repository() {
        return createOptional(getAggregateAnnotation().repository());
    }

    String repositoryName() {
        return repository().orElse(lcFirst(aggregateType().getSimpleName()) + "Repository");
    }

    Optional<String> snapshotTriggerDefinition() {
        return createOptional(getAggregateAnnotation().snapshotTriggerDefinition());
    }

    Optional<String> type() {
        return createOptional(getAggregateAnnotation().type());
    }

    Optional<String> commandTargetResolver() {
        return createOptional(getAggregateAnnotation().commandTargetResolver());
    }

    private Aggregate getAggregateAnnotation() {
        return aggregateType.getAnnotation(Aggregate.class);
    }

    boolean isJpaAggregate() {
        return Arrays.stream(aggregateType.getAnnotations())
                     .map(Annotation::annotationType)
                     .map(Class::getName)
                     .anyMatch("javax.persistence.Entity"::equals);
    }

    // TODO: 8/29/2018 extract to some util and replace in SagaDefinition too
    private String lcFirst(String string) {
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    // TODO: 8/29/2018 extract to some util and replace in SagaDefinition too
    private Optional<String> createOptional(String value) {
        if ("".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
