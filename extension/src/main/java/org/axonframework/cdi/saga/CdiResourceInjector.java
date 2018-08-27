package org.axonframework.cdi.saga;

import org.axonframework.eventhandling.saga.ResourceInjector;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * @author Milan Savic
 */
public class CdiResourceInjector implements ResourceInjector {

    private final BeanManager beanManager;

    public CdiResourceInjector(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void injectResources(Object saga) {
        CreationalContext creationalContext = beanManager.createCreationalContext(null);

        AnnotatedType annotatedType = beanManager.createAnnotatedType(saga.getClass());
        InjectionTarget injectionTarget = beanManager.createInjectionTarget(annotatedType);
        injectionTarget.inject(saga, creationalContext);
    }
}
