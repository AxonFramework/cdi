package org.axonframework.cdi;

import org.axonframework.cdi.stereotype.Aggregate;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Milan Savic
 */
public class AggregateDefinition {

    private final Class<?> aggregateType;

    public AggregateDefinition(Class<?> aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Class<?> aggregateType() {
        return aggregateType;
    }

    public Optional<String> repository() {
        return StringUtilities.createOptional(getAggregateAnnotation().repository());
    }

    public String repositoryName() {
        return repository().orElse(StringUtilities.lowerCaseFirstLetter(
                aggregateType().getSimpleName()) + "Repository");
    }

    public Optional<String> snapshotTriggerDefinition() {
        return StringUtilities.createOptional(getAggregateAnnotation()
                .snapshotTriggerDefinition());
    }

    public Optional<String> type() {
        return StringUtilities.createOptional(getAggregateAnnotation().type());
    }

    public Optional<String> commandTargetResolver() {
        return StringUtilities.createOptional(getAggregateAnnotation()
                .commandTargetResolver());
    }

    private Aggregate getAggregateAnnotation() {
        return aggregateType.getAnnotation(Aggregate.class);
    }

    public boolean isJpaAggregate() {
        return Arrays.stream(aggregateType.getAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getName)
                .anyMatch("javax.persistence.Entity"::equals);
    }
}
