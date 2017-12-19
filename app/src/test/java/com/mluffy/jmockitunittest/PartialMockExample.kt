package com.mluffy.jmockitunittest

import mockit.Expectations
import mockit.integration.junit4.JMockit
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Created by mluhui on 18/12/2017.
 */
@RunWith(JMockit::class)
class PartialMockExample {
    class Collaborator {
        val mValue: Int
        val value: Int
            get() = mValue

        constructor() {
            mValue = -1
        }

        constructor(value: Int) {
            mValue = value
        }

        fun simpleOperation(a: Int, b: String, c: Date?): Boolean {
            return true
        }

        companion object {
            fun doSomething(b: Boolean, s: String) {
                throw IllegalStateException()
            }
        }
    }

    @Test
    fun partiallyMockingAClassAndItsInstances() {
        //这里只是为了拿到一个实例，用于在 expectation 里使用，随便哪个实例都行，即使是后边拿来测试用的
        val anyInstance = Collaborator()

        //注意，这里必须用 class.java
        object : Expectations(Collaborator::class.java) {
            init {
                anyInstance.value
                result = 123
            }
        }

        // 正常的构造，会调用我们的实现
        val c1 = Collaborator()
        val c2 = Collaborator(150)

        // 以下调用了 mocked 的方法，即都会返回123
        assertEquals(123, c1.value)
        assertEquals(123, c2.value)

        // 调用没有在 expectation 中定义的方法，则会返回我们自己的实现
        assertTrue(c1.simpleOperation(1, "b", null))
        assertEquals(45, Collaborator(45).mValue)
    }

    @Test
    fun partiallyMockingASingleInstance() {
        val collaborator = Collaborator(2)
        //构造参数中传入 collaborator 实例，表明只 mock 该对象
        object : Expectations(collaborator) {
            init {
                collaborator.value
                result = 123
                collaborator.simpleOperation(1, "", null)
                result = false
            }
        }

        //static 方法的 mock 方式，详细说明参见 Kotlin 中的坑 -> static function mock
        object : Expectations(Collaborator.Companion) {
            init {
                // Static methods can be dynamically mocked too.
                Collaborator.doSomething(anyBoolean, "test")
            }
        }

        // 调用 collaborator 对象的对应方法，都会使用 mocked 的结果
        assertEquals(123, collaborator.value)
        assertFalse(collaborator.simpleOperation(1, "", null))
        Collaborator.doSomething(true, "test")

        // 调用没有在 expectation 中的方法，还是正常的
        assertEquals(2, collaborator.mValue)

        // 用其他实例调用 mocked 方法，还是返回该对象正常的实现
        assertEquals(45, Collaborator(45).mValue)
        assertEquals(-1, Collaborator().value)
    }
}