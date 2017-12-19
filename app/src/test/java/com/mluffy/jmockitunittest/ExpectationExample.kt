package com.mluffy.jmockitunittest

import mockit.Expectations
import mockit.Mocked
import mockit.integration.junit4.JMockit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by mluhui on 18/12/2017.
 */

@RunWith(JMockit::class)
class ExpectationExample {
    class SomeClass {
        fun methodToBeMock() = 1
        companion object {
            fun staticFunction() = 110
        }
    }

    @Test
    fun mockInstanceExpectationTest(@Mocked mockInstance: SomeClass, @Mocked companion: SomeClass.Companion/*在这里加个 companion 是因为 kotlin 里没有 static 方法，所有看起来像 static 调用的方法，实际上都是声明在了内部类 Companion 中了*/) {
        // 重写一个 mock 对象的返回值
        object : Expectations() {
            init {
                //1. 调用一个对象的方法
                mockInstance.methodToBeMock()
                //2. 重新定义返回值，当然返回类型必须和声明的一致，Unit 则不需要写
                result = 123
                //3. 设定 mock 生效次数，times = 1 则说明只生效一次，不写则不限定匹配次数
                times = 1

                //4. 也可以重写 static 方法
                SomeClass.staticFunction()
                result = 1
            }
        }

        assertEquals(123, mockInstance.methodToBeMock()) //测试通过
        assertEquals(1, SomeClass.staticFunction()) // 测试通过
        assertEquals(123, mockInstance.methodToBeMock()) //测试不通过，因为在 expectation 中只设定了 times = 1，第二次会抛出异常，Unexpected invocation
    }
}