package org.axonframework.cdi;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import org.jboss.weld.junit4.WeldInitiator;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;

public class CdiUtilitiesTest {

    @Rule
    public WeldInitiator weld = WeldInitiator.from(Foo.class).inject(this).build();

    @Inject
    private BeanManager beanManager;

    @Inject
    Foo foo;

    @Test
    public void testGetBeanManager() {
        assertNotNull(CdiUtilities.getBeanManager());
    }

    @Test
    public void testGetReference() {
        Set<Bean<?>> beansFound = beanManager.getBeans(Foo.class,
                new AnnotationLiteral<Default>() {
        });
        assertSame(foo, CdiUtilities.getReference(beanManager,
                beansFound.iterator().next(), Foo.class));
    }

    @Test
    public void hasAnnotatedMember_true() {
        assertTrue(CdiUtilities.hasAnnotatedMember(new HasAnnotatedField(),
                FirstFieldAnnotation.class));
    }

    @Test
    public void hasAnnotatedMember_false() {
        assertFalse(CdiUtilities.hasAnnotatedMember(new HasNoAnnotatedField(),
                FirstFieldAnnotation.class));
    }

    @Test
    public void hasAnnotatedMember_both_false() {
        assertFalse(CdiUtilities.hasAnnotatedMember(new HasAnnotatedField(),
                FirstFieldAnnotation.class, SecondFieldAnnotation.class));
    }

    @Test
    public void hasAnnotatedMember_both_true() {
        assertTrue(CdiUtilities.hasAnnotatedMember(new HasBothFieldAnnotations(),
                FirstFieldAnnotation.class, SecondFieldAnnotation.class));
    }

/*X TODO Reza: hasAnnotatedMethod now takes an AnnotatedType not a Bean.
    @Test
    public void hasAnnotatedMethod_true() {
        assertTrue(CdiUtilities.hasAnnotatedMethod(new HasMethodAnnotation(),
                FirstMethodAnnotation.class));
    }

    @Test
    public void hasAnnotatedMethodInParent_true() {
        assertTrue(CdiUtilities.hasAnnotatedMethod(new InheritAnnotatedMethod(),
                FirstMethodAnnotation.class));
    }

    @Test
    public void hasAnnotatedPrivateMethod_true() {
        assertTrue(CdiUtilities.hasAnnotatedMethod(new HasPrivateMethodAnnotation(),
                FirstMethodAnnotation.class));
    }

    @Test
    public void hasAnnotatedPrivateMethodInParent_true() {
        assertTrue(CdiUtilities.hasAnnotatedMethod(new InheritAnnotatedPrivateMethod(),
                FirstMethodAnnotation.class));
    }

    @Test
    public void hasAnnotatedMethod_false() {
        assertFalse(CdiUtilities.hasAnnotatedMethod(new HasMethodAnnotation(),
                SecondMethodAnnotation.class));
    }
*/

    @ApplicationScoped
    private static class Foo implements Serializable {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FirstFieldAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface SecondFieldAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface FirstMethodAnnotation {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface SecondMethodAnnotation {
    }

    public static class HasAnnotatedField extends BeanWrapper<HasAnnotatedField> {

        @FirstFieldAnnotation
        protected String name;

        public HasAnnotatedField() {
            super(HasAnnotatedField.class, HasAnnotatedField::new);
        }
    }

    public static class HasMethodAnnotation extends BeanWrapper<HasMethodAnnotation> {

        public HasMethodAnnotation() {
            super(HasMethodAnnotation.class, HasMethodAnnotation::new);
        }

        @FirstMethodAnnotation
        public void foo() {

        }
    }

    public static class InheritAnnotatedMethod extends HasMethodAnnotation {
        public void bar() {

        }
    }

    public static class HasPrivateMethodAnnotation extends BeanWrapper<HasMethodAnnotation> {

        public HasPrivateMethodAnnotation() {
            super(HasMethodAnnotation.class, HasMethodAnnotation::new);
        }

        @FirstMethodAnnotation
        private void foo() {

        }
    }

    public static class InheritAnnotatedPrivateMethod extends HasPrivateMethodAnnotation {
        public void bar() {

        }
    }

    public static class HasBothFieldAnnotations extends BeanWrapper<HasBothFieldAnnotations> {

        @FirstFieldAnnotation
        @SecondFieldAnnotation
        protected String name;

        public HasBothFieldAnnotations() {
            super(HasBothFieldAnnotations.class, HasBothFieldAnnotations::new);
        }
    }

    public static class InheritAnnotatedField extends HasAnnotatedField {

        private String other;
    }

    public static class HasNoAnnotatedField extends BeanWrapper<HasNoAnnotatedField> {

        private String name;

        public HasNoAnnotatedField() {
            super(HasNoAnnotatedField.class, HasNoAnnotatedField::new);
        }
    }
}
