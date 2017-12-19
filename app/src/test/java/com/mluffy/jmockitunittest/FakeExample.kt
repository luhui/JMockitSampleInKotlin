package com.mluffy.jmockitunittest

import android.util.Log
import junit.framework.Assert
import mockit.Mock
import mockit.MockUp
import mockit.Tested
import mockit.integration.junit4.JMockit
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by mluhui on 18/12/2017.
 * MockUp 的基本使用，一般用于 external library。它和 mock 是很相似的，一般 fake 是可复用的，多在 test 初始化（@before, @beforeClass）时调用。而 mock 是跟随 test case 的。
 */
@RunWith(JMockit::class)
class FakeExample {
    class SomeClass {
        fun doSomething() = Utils.doSomething()
        fun doOtherThing() = Utils().doOtherThing()
    }

    class Utils {
        fun doOtherThing() = 2
        companion object {
            fun doSomething() = 1
        }
    }

    //fake 一个成员方法
    class FakeUtils: MockUp<Utils>() {
        @Mock
        fun doOtherThing() = 22
    }

    //fake 一个 Companion 方法，因为 class 是一个 kotlin class，因此需要 fake 的是Companion，这和 Kotlin 生成的 java 代码相关，和 Mock 一个 static 方法类似
    class FakeUtilsCompanion: MockUp<Utils.Companion>() {
        @Mock
        fun doSomething() = 11
    }

    //因为 Log 是 java 实现，所以 static 方法我们可以按照官网的例子去 fake
    class FakeLog: MockUp<Log>() {
        companion object {
            @JvmStatic
            @Mock
            fun e(tag: String, message: String): Int {
                print("log message = $message")
                return 0
            }
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            FakeUtils()
            FakeUtilsCompanion()
            FakeLog()
        }
    }

    @Test
    fun testFake(@Tested testInstance: SomeClass) {
        Log.e("tag", "print a message in console!")
        Assert.assertEquals(11, testInstance.doSomething())
        Assert.assertEquals(22, testInstance.doOtherThing())
    }
}