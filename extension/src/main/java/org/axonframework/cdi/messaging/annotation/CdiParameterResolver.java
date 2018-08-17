package org.axonframework.cdi.messaging.annotation;

import org.axonframework.cdi.CdiUtilities;
import org.axonframework.messaging.annotation.ParameterResolver;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.reflect.Type;
import org.axonframework.messaging.Message;

public class CdiParameterResolver implements ParameterResolver<Object> {

    private final BeanManager beanManager;
    private final Bean<?> bean;
    private final Type type;

    public CdiParameterResolver(final BeanManager beanManager,
            final Bean<?> bean, final Type type) {
        this.beanManager = beanManager;
        this.bean = bean;
        this.type = type;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object resolveParameterValue(final Message message) {
        return CdiUtilities.getReference(beanManager, bean, type);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean matches(final Message message) {
        return true;
    }
}
