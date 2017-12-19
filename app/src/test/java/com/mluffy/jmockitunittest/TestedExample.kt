package com.mluffy.jmockitunittest

import mockit.Injectable
import mockit.Tested
import mockit.integration.junit4.JMockit
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by mluhui on 18/12/2017.
 * Tested 的使用，他会自动根据 constructor 构造实例
 * 当 constructor 有多个参数时，他会根据 injectable 的数量以及参数名来匹配
 */
@RunWith(JMockit::class)
class TestedExample {
    class SomeClass {
        private var value: Int
        private var value2: Int = 0
        private var value3: Int = 0
        constructor() {
            value = 0
        }
        constructor(value: Int, value2: Int): this() {
            this.value = value
            this.value2 = value2
        }
        constructor(value: Int, value2: Int, value3: Int): this(value, value2) {
            this.value3 = value3
        }
        fun doSomething() = value
        fun doSomething2() = value2
        fun doSomething3() = value3
    }

    @Test
    //调用无参构造函数
    fun testDoSomethingWithoutParams(@Tested testInstance: SomeClass) {
        Assert.assertEquals(0, testInstance.doSomething())
    }

    @Test
    //调用有两个参数的构造函数
    fun testDoSomethingWith2Prams(@Tested testInstance: SomeClass, @Injectable(value = "102") value2:Int, @Injectable(value = "100") value: Int) {
        Assert.assertEquals(100, testInstance.doSomething())
        Assert.assertEquals(102, testInstance.doSomething2())
    }

    @Test
    //调用有三个参数的构造函数
    fun testDoSomethingWith3Prams(@Tested testInstance: SomeClass, @Injectable(value = "102") value2:Int, @Injectable(value = "100") value: Int, @Injectable(value = "103") value3: Int) {
        Assert.assertEquals(100, testInstance.doSomething())
        Assert.assertEquals(102, testInstance.doSomething2())
        Assert.assertEquals(103, testInstance.doSomething3())
    }

    @Test
    //把 value3改为 value4，实际上会匹配到接收两个参数的构造函数，等价于 testInstance = SomeClass(100, 102)，value4是没有用到的
    fun testDoSomethingWithDifferentName(@Tested testInstance: SomeClass, @Injectable(value = "102") value2:Int, @Injectable(value = "100") value: Int, @Injectable(value = "103") value4: Int) {
        Assert.assertEquals(100, testInstance.doSomething())
        Assert.assertEquals(102, testInstance.doSomething2())
        Assert.assertEquals(0, testInstance.doSomething3())
    }
}