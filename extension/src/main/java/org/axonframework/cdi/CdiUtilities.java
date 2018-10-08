package org.axonframework.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import static java.util.Arrays.stream;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMember;
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
    public static <T> T getReference(final BeanManager beanManager,
            final Bean<T> bean,
            final Type beanType) {
        return (T) beanManager.getReference(bean, beanType,
                beanManager.createCreationalContext(bean));
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
     * @param bean bean to check.
     * @param clazz annotation class.
     * @return true if at least one annotated method is present.
     */
    public static final boolean hasAnnotatedMethod(final Bean<?> bean,
            final Class<? extends Annotation> clazz) {
        return getDeclaredMethodsTransitive(bean.getBeanClass()).anyMatch(
                m -> m.isAnnotationPresent(clazz));
    }

    private static final boolean hasOneOfTheseAnnotations(final Method m, final Class<? extends Annotation>... classes) {
        return Arrays.stream(classes).anyMatch(c -> m.isAnnotationPresent(c));
    }

    /**
     * Checks whether a given bean has methods annotated with given annotation.
     *
     * @param beanClass Class of the bean to check.
     * @param classes annotation class.
     * @return true if at least one annotated method is present.
     */
    public static final boolean hasAnnotatedMethod(final Class<?> beanClass,
                                                   final Class<? extends Annotation>... classes) {
        return getDeclaredMethodsTransitive(beanClass).anyMatch(
                m -> hasOneOfTheseAnnotations(m, classes));
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

    public static String extractBeanName(AnnotatedMember<?> annotatedMember) {
        Named named = annotatedMember.getAnnotation(Named.class);

        if (named != null && !"".equals(named.value())) {
            return named.value();
        }

        // TODO: Should not try to derive the name of a member that does not
        // have the @Named annotation on it.
        return annotatedMember.getJavaMember().getName();
    }

    /**
     * Returns a transitive stream of all methods of a class, for the purpose of
     * scanning for methods with a given annotation. As we know Object will not
     * have Axon annotations, either that or null is a reason to stop traveling
     * upwards in the hierarchy.
     *
     * Added this because Class<>.getMethods() only returns a transitive list of
     * public methods.
     *
     * @param clazz The starting point in the hierarchy.
     * @return An empty stream for null or java.lang.Object, otherwise a stream
     * of all methods (public/protected/package private/private) followed by
     * those of its super, etc.
     */
    // TODO: See if this is really necessary, manifestation of a bug elsewhere
    // or is a quirk of CDI. Does have a performance cost.
    private static Stream<Method> getDeclaredMethodsTransitive(Class<?> clazz) {
        return ((clazz == null) || clazz.equals(java.lang.Object.class))
                ? Stream.empty()
                : Stream.concat(stream(clazz.getDeclaredMethods()),
                        getDeclaredMethodsTransitive(clazz.getSuperclass()));
    }
}
