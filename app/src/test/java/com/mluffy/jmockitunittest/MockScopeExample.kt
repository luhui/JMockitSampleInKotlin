package com.mluffy.jmockitunittest

import mockit.Mocked
import mockit.integration.junit4.JMockit
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by mluhui on 18/12/2017.
 */
@RunWith(JMockit::class)
class MockScopeExample {
//    @Mocked lateinit var anyInstance: SomeClass
    class SomeClass {
        fun doSomething() = 1
    }

    @Test
    fun test1(@Mocked mockInstance: SomeClass) {
        Assert.assertEquals(0, mockInstance.doSomething())
    }

    @Test
    fun test2() {
        val instance = SomeClass()
        Assert.assertEquals("should be fail when uncommon anyInstance declaration", 1, instance.doSomething())
    }
}