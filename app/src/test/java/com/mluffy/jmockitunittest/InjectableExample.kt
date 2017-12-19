package com.mluffy.jmockitunittest

import junit.framework.Assert
import mockit.Expectations
import mockit.Injectable
import mockit.Tested
import mockit.integration.junit4.JMockit
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by mluhui on 18/12/2017.
 * Injectable 用法。Injectable 是一个 mocked 对象，声明 Injectable 不会像 Mocked 一样，他只会影响被标记的对象。一般和@Tested 配合使用，作为依赖注入的参数
 */
@RunWith(JMockit::class)
class InjectableExample {
    class SomeClass(private val a: DependencyA) {
        fun plus1() = a.value() + 1
    }

    class DependencyA {
        fun value() = 1
    }

    @Test
    fun testPlus1(@Tested testInstance: SomeClass, @Injectable a: DependencyA) {
        //a 是一个 mocked 对象，int 返回必定是0
        Assert.assertEquals(0, a.value())
        //a 是一个 mocked 对象，可以修改 value 的返回值
        object : Expectations() {
            init {
                a.value()
                result = 2
            }
        }
        //修改了 a.value 的结果，所以预期的结果是 2 + 1 = 3
        Assert.assertEquals(3, testInstance.plus1())

        //因为 Injectable 只是标识对应的实例是 mocked，不影响其他实例，所以生成一个新的 a，a.value()返回的是1
        Assert.assertEquals(1, DependencyA().value())
    }

    @Test
    fun testPlus1WithCustomCreation() {
        val a = DependencyA()
        //Mock 失败，因为a 不是一个 mock 对象
        object : Expectations() {
            init {
                a.value()
                result = 2
            }
        }
        val testInstance = SomeClass(a)
        Assert.assertEquals(2, testInstance.plus1())
    }
}