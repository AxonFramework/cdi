package org.axonframework.cdi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Supplier;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BeanWrapperTest {

    private final Supplier<Foo> supplier = () -> new Foo();
    private final BeanWrapper<Foo> testee = new BeanWrapper<>(Foo.class, supplier);

    @Test
    public void testId() {
        assertEquals(Foo.class.toString() + "#" + supplier.toString(), testee.getId());
    }

    @Test
    public void testCreate() {
        assertEquals(new Foo(), testee.create(null));
    }

    // Antoine: How do you think I can test this?
    @Test
    public void testDestroy() {
        // TODO.
    }

    @Test
    public void testName() {
        assertEquals(Foo.class.getSimpleName(), testee.getName());
    }

    @Test
    public void testGetStereotypes() {
        assertTrue(testee.getStereotypes().isEmpty());
    }

    @Test
    public void testGetTypes() {
        assertEquals(new HashSet<>(Arrays.asList(Foo.class, Object.class)),
                testee.getTypes());
    }

    @Test
    public void testBeanClass() {
        assertEquals(Foo.class, testee.getBeanClass());
    }

    @Test
    public void testScope() {
        assertEquals(ApplicationScoped.class, testee.getScope());
    }

    @Test
    public void testQualifiers() {
        assertEquals(new HashSet<>(Arrays.asList(new AnnotationLiteral<Default>() {
        }, new AnnotationLiteral<Any>() {
        })), testee.getQualifiers());
    }

    @Test
    public void testInjectionPoints() {
        assertTrue(testee.getInjectionPoints().isEmpty());
    }

    @Test
    public void testIsAlternative() {
        assertFalse(testee.isAlternative());
    }

    @Test
    public void testIsNullable() {
        assertFalse(testee.isNullable());
    }

    private static class Foo {

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Foo);
        }

        @Override
        public int hashCode() {
            return 7;
        }

        @Override
        public String toString() {
            return "Foo";
        }
    }
}
