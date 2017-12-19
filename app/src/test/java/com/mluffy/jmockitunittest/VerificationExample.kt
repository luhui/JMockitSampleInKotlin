package com.mluffy.jmockitunittest

import android.content.Context
import android.content.Intent
import android.util.Log
import mockit.*
import mockit.integration.junit4.JMockit
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by mluhui on 18/12/2017.
 * Verification 的用法，和 Expectation 使用相似，但是只能 verify mocked 的对象（包括 partial mock）,但是不支持 fake class
 */
@RunWith(JMockit::class)
class VerificationExample {
    class SomeClass(val context: Context) {
        fun doSomething() {
            Log.e("error", "some thing happened")
        }

        fun doOtherThing() {
            Log.d("tag", "some log")
            context.startActivity(Intent())
        }

        fun doMoreThing() {
            context.sendBroadcast(Intent())
            Log.d("tag", "some log")
            context.startActivity(Intent())
        }

        fun callSomething() {
            UtilsClass.doSomething()
        }
    }

    class UtilsClass {
        companion object {
            fun doSomething() = 1
            fun doSomething2() = 2
        }
    }

    @Injectable
    private lateinit var anyContext: Context
    //必须要包含@Mocked anyLog: Log，这样才能让 Log 里的所有方法被 mocked，Verification 才能生效
    @Mocked
    private lateinit var anyLog: Log
    @Tested
    private lateinit var testInstance: SomeClass

    @Test

    fun testDoSomething() {
        testInstance.doSomething()
        object : Verifications() {
            init {
                Log.e("error", anyString)
                times = 1
            }
        }
    }

    @Test
    fun testDoOtherThing(@Mocked anyIntent: Intent) {
        testInstance.doOtherThing()
        //测试方法的调用顺序，可以不包含所有的方法调用
        object : VerificationsInOrder() {
            init {
                Log.d(anyString, anyString)
                times = 1
                anyContext.startActivity(any as Intent?)
                times = 1
            }
        }

        //测试都调用了哪些方法，不需要理会顺序
        object : Verifications() {
            init {
                anyContext.startActivity(any as Intent?)
                times = 1
                Log.d(anyString, anyString)
                times = 1
            }
        }
    }

    @Test
    //测试方法的调用顺序，必须包含所有的方法调用。一般很少用到这个方式测试
    fun testDoMoreThing(@Mocked anyIntent: Intent) {
        testInstance.doMoreThing()
        object : FullVerificationsInOrder() {
            init {
                //里面的任何方法调用都需要写，否则测试不通过
                Intent()
                anyContext.sendBroadcast(any as Intent?)
                Log.d(anyString, anyString)
                Intent()
                anyContext.startActivity(any)
            }
        }
    }

    @Test
    //PartialMock 的对象，也可以 verify
    fun testWithPartialMock() {
        object : Expectations(UtilsClass.Companion) {
            init {
                UtilsClass.doSomething()
            }
        }
        testInstance.callSomething()
        object : VerificationsInOrder() {
            init {
                UtilsClass.doSomething()
//                UtilsClass.doSomething2() //doSomething2虽然没有被 mock，但是也可以支持 verify 了
                times = 1
            }
        }
    }
}