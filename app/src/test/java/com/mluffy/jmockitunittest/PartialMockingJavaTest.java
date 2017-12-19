package com.mluffy.jmockitunittest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import mockit.Expectations;
import mockit.integration.junit4.JMockit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mluhui on 18/12/2017.
 */

@RunWith(JMockit.class)
public class PartialMockingJavaTest
{
    static class Collaborator
    {
        final int value;

        Collaborator() { value = -1; }
        Collaborator(int value) { this.value = value; }

        int getValue() { return value; }
        final boolean simpleOperation(int a, String b, Date c) { return true; }
        static void doSomething(boolean b, String s) { throw new IllegalStateException(); }
    }

    @Test
    public void partiallyMockingAClassAndItsInstances() {
        final Collaborator anyInstance = new Collaborator();

        new Expectations(Collaborator.class) {{
            anyInstance.getValue(); result = 123;
        }};

        // Not mocked, as no constructor expectations were recorded:
        Collaborator c1 = new Collaborator();
        Collaborator c2 = new Collaborator(150);

        // Mocked, as a matching method expectation was recorded:
        assertEquals(123, c1.getValue());
        assertEquals(123, c2.getValue());

        // Not mocked:
        assertTrue(c1.simpleOperation(1, "b", null));
        assertEquals(45, new Collaborator(45).value);
    }

    @Test
    public void partiallyMockingASingleInstance() {
        final Collaborator collaborator = new Collaborator(2);

        new Expectations(collaborator) {{
            collaborator.getValue(); result = 123;
            collaborator.simpleOperation(1, "", null); result = false;

            // Static methods can be dynamically mocked too.
            Collaborator.doSomething(anyBoolean, "test");
        }};

        // Mocked:
        assertEquals(123, collaborator.getValue());
        assertFalse(collaborator.simpleOperation(1, "", null));
        Collaborator.doSomething(true, "test");

        // Not mocked:
        assertEquals(2, collaborator.value);
        assertEquals(45, new Collaborator(45).getValue());
        assertEquals(-1, new Collaborator().getValue());
    }
}
