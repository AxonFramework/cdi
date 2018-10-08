package org.axonframework.cdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import java.lang.invoke.MethodHandles;
import java.util.Set;

public class BeanLifecycleInterceptor<T> implements InjectionTarget<T> {

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    private InjectionTarget<T> target;

    public BeanLifecycleInterceptor(InjectionTarget<T> target) {
        this.target = target;
    }

    @Override
    public void inject(T t, CreationalContext<T> creationalContext) {
        target.inject(t, creationalContext);
    }

    @Override
    public void postConstruct(T t) {
        logger.debug("postConstruct({})", t.getClass().getName());

        target.postConstruct(t);
    }

    @Override
    public void preDestroy(T t) {
        logger.debug("preDestroy({})", t.getClass().getName());

        target.preDestroy(t);
    }

    @Override
    public T produce(CreationalContext<T> creationalContext) {
        return target.produce(creationalContext);
    }

    @Override
    public void dispose(T t) {
        target.dispose(t);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return target.getInjectionPoints();
    }
}
