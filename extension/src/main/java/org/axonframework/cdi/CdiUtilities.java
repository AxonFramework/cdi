package org.axonframework.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import static java.util.Arrays.stream;
import java.util.function.Predicate;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

// TODO Should this be made package private to limit API visibility?
public class CdiUtilities {

    /**
     * Returns an object reference of a given bean.
     *
     * @param beanManager bean manager.
     * @param bean bean.
     * @param beanType bean type.
     * @return Object reference.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getReference(final BeanManager beanManager,
            final Bean<T> bean,
            final Type beanType) {
        return (T) beanManager.getReference(bean, beanType,
                beanManager.createCreationalContext(bean));
    }

    /**
     * Checks whether a given bean has methods annotated with given annotation.
     *
     * @param bean bean to check.
     * @param clazz annotation class.
     * @return true if at least one annotated method is present.
     */
    public static final boolean hasAnnotatedMethod(final Bean<?> bean,
            final Class<? extends Annotation> clazz) {
        return stream(bean.getBeanClass().getMethods()).anyMatch(
                m -> m.isAnnotationPresent(clazz));
    }

    /**
     * Checks whether a bean has a member annotated with all provided
     * annotations.
     *
     * @param bean bean to check.
     * @param classes annotation classes to check for.
     * @return true if a member of a bean is annotated with all specified
     * annotations.
     */
    @SafeVarargs
    public static boolean hasAnnotatedMember(final Bean<?> bean,
            final Class<? extends Annotation>... classes) {
        final Predicate<Field> hasAllAnnotations
                = field -> stream(classes).allMatch(field::isAnnotationPresent);
        return stream(bean.getBeanClass().getDeclaredFields()).anyMatch(
                hasAllAnnotations);
    }

    /**
     * Retrieve the bean manager.
     *
     * @return bean manager, if any, or <code>null</code>.
     */
    public static BeanManager getBeanManager() {
        return CDI.current().getBeanManager();
    }
}
