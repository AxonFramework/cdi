package org.axonframework.cdi.messaging.annotation;

import java.lang.invoke.MethodHandles;
import org.axonframework.cdi.CdiUtilities;
import org.axonframework.common.Priority;
import org.axonframework.messaging.annotation.ParameterResolver;
import org.axonframework.messaging.annotation.ParameterResolverFactory;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameter resolver factory for instantiating Axon artifacts inside of a CDI
 * context.
 *
 * @author Simon Zambrovski
 */
@Priority(Priority.LOW)
public class CdiParameterResolverFactory implements ParameterResolverFactory {

    private final static Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private final BeanManager beanManager;

    public CdiParameterResolverFactory() {
        this.beanManager = CdiUtilities.getBeanManager();
    }

    @Override
    public ParameterResolver<?> createInstance(final Executable executable,
            final Parameter[] parameters, final int parameterIndex) {
        final Parameter parameter = parameters[parameterIndex];

        if (this.beanManager == null) {
            logger.error(
                    "BeanManager was null. This is a fatal error, an instance of {} {} is not created.",
                    parameter.getType(),
                    parameter.getAnnotations());
            return null;
        }

        logger.trace("Create instance for {} {}.", parameter.getType(), 
                parameter.getAnnotations());
        
        final Set<Bean<?>> beansFound = beanManager.getBeans(parameter.getType(), 
                parameter.getAnnotations());
        
        if (beansFound.isEmpty()) {
            return null;
        } else if (beansFound.size() > 1) {
            if (logger.isWarnEnabled()) {
                logger.warn("Ambiguous reference for parameter type {} with qualifiers {}.", 
                        parameter.getType().getName(), parameter.getAnnotations());
            }
            
            return null;
        } else {
            return new CdiParameterResolver(beanManager, 
                    beansFound.iterator().next(), parameter.getType());
        }
    }
}
