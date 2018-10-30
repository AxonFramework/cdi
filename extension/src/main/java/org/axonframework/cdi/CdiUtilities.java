package org.axonframework.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import static java.util.Arrays.stream;
import java.util.Set;
import java.util.function.Predicate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Named;

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
    public static <T> T getReference(final BeanManager beanManager, final Bean<T> bean,
            final Type beanType) {
        return (T) beanManager.getReference(bean, beanType,
                beanManager.createCreationalContext(bean));
    }

    public static Bean resolveBean(final BeanManager beanManager, final Class clazz) {
        final Set<Bean<?>> beans = beanManager.getBeans(clazz);

        if (!beans.isEmpty()) {
            return beanManager.resolve(beans);
        }

        return null;
    }

    public static <T> T getReference(final BeanManager beanManager, final Bean<T> bean) {
        final CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
    }

    public static <T> T getReference(final BeanManager beanManager, final String name) {
        final Set<Bean<?>> beans = beanManager.getBeans(name);
        final Bean<?> bean = beanManager.resolve(beans);

        final CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
    }

    public static <T> T getReference(final BeanManager beanManager, final Class<T> clazz) {
        final Set<Bean<?>> beans = beanManager.getBeans(clazz);
        final Bean<?> bean = beanManager.resolve(beans);

        final CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, clazz, creationalContext);
    }

    /**
     * Checks whether a given bean has methods annotated with given annotation.
     *
     * @param at AnnotatedType to check.
     * @param annotationClazz annotation class.
     * @return true if at least one annotated method is present.
     */
    public static final boolean hasAnnotatedMethod(final AnnotatedType<?> at,
            final Class<? extends Annotation> annotationClazz) {
        return at.getMethods().stream().anyMatch(m -> m.isAnnotationPresent(annotationClazz));
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

    static String extractBeanName(AnnotatedMember<?> annotatedMember) {
        Named named = annotatedMember.getAnnotation(Named.class);

        if (named != null && !"".equals(named.value())) {
            return named.value();
        }

        // TODO Should not try to derive the name of a member that does not
        // have the @Named annotation on it.
        return annotatedMember.getJavaMember().getName();
    }
}
