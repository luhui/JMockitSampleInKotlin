# 前言

这是一篇有关 JMockit 常用API的介绍，以及在 kotlin 中的一些注意事项。此文章是基于 JMockit 1.37版本，Kotlin 1.2版本编写的。

[JMockit tutorial](http://jmockit.org/tutorial)其实写得很详细了，这里只是把平常在项目中用到的方式列举出来详细说明，以及官网上没提到的 kotlin 下的一些坑。所以这里算是个导读，想要精通，还是建议把 [tutorial](http://jmockit.org/tutorial) 看完。

# Annoation

## Mocked

这个对象的实例是由 JMockit 生成的，标记了`@Mocked`之后，当前 context 下的所有该类的对象，都会被使用相同的对象，所有的方法调用都不会再执行原有的实现。

`@Mocked`有两种使用，对用不同的生效周期

```kotlin
@RunWith(JMockit::class)
class MockScope {
    //声明为成员变量，则所有 MockScope 下的 test case，其 SomeClass 的实例都是一个 mocked 实例，所有方法调用都不是我们自己的实现
//    @Mocked lateinit var anyInstance: SomeClass
    class SomeClass {
        fun doSomething() = 1
    }

    @Test
    //每一个 test case 都可以声明不定长的参数，这些参数可以声明为@Mocked，@Injectable 等。其生效的周期和该 test case 生命周期一致
    fun test1(@Mocked mockInstance: SomeClass) {
        //一个 mocked 实例返回的 int 值永远是0
        Assert.assertEquals(0, mockInstance.doSomething())
    }

    @Test
    fun test2() {
        //如果没有取消成员变量 anyInstance 的注释，则测试通过，取消注释后，instance.doSomething 返回的是0
        val instance = SomeClass()
        Assert.assertEquals("should be fail when uncommon anyInstance declaration", 1, instance.doSomething())
    }
}
```

## Tested

Tested 是标记了一个对象是测试对象，如果没有初始化，他会根据构造函数自动生成对应的实例。参数的注入通过 Injectable 来配合，需要类型和名称都一一匹配，Injectable 会生成一个 mock 对象，详情参见下一小节。

```kotlin
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
```

重点看最后一个 case，我们声明了一个 value4，没有匹配上 constructor，所以框架只找到两个匹配参数，因此调用了两个参数的构造函数。

## Injectable

Injectable 是一个 mocked 对象，声明 Injectable 不会像 Mocked 一样，他只会影响被标记的对象。一般和@Tested 配合使用，作为依赖注入的参数

```kotlin
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
```

## Mock

用于标记一个方法，声明这个方法是一个 mock 方法，是被 mocked 对象对应方法的实现。具体使用参见下面的[Faking](#Faking)一节

```kotlin
class SomeLib {
  fun doSomething() = 1
}
class FakeLog: MockUp<Log> {
  @Mock
  fun doSomething() = 2
}
```

## 小结

1. Annotation 可以声明成员变量，会影响所有该 test suit 下的所有 testcase
2. Annotation 可以声明单个 testcase 的方法参数，他仅在该 testcase 下生效
3. Mocked 是用于标记class 的，被标记的 class 下的所有对象都会变为 mocked 对象，所有调用方法都不再调用原有的实现。其生效周期参见第2点
4. Tested 是给一个测试对象增加声明，在没有初始化时，框架会帮助我们自动生成对象，一般配合 injectable 使用，类型和名称需要一一匹配
4. Injectable 可以认为是一个特殊的 Mocked 对象，他不同于 Mocked，只会对被标记的对象生效，被标记的对象会变为一个 mocked 对象
5. Mock 需要配合 MockUp 使用，一般用于 Mock 第三方库的方法。可以认为是Partical Mock 的另一种实现，当然他们的适用范围不同

# Mocking

## Expectation

顾名思义，即重写一个对象的行为，让他符合我们的预期。 他只能用于一个 test case 中（即@Test 标记的 fun)

###  通常使用

```kotlin
@RunWith(JMockit::class)
class Expectation {
    class SomeClass {
        fun methodToBeMock() = 1
        companion object {
            fun staticFunction() = 110
        }
    }

    @Test
    //在测试参数里，加了一个 companion 的 mock，这是kotlin 下 static function mock 的解决方案，详情参见下方 static function mock
    fun mockInstanceExpectationTest(@Mocked mockInstance: SomeClass, @Mocked companion: SomeClass.Companion) {
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
```

### PartialMock

在默认情况下，Expectations 只能用于被标记为`@Mocked`的对象，当我们需要使用某个对象原有的方法，而部分方法又需要使用 mock 时，就需要使用到 partial mock 了。

partial mock 有两种使用方式，一个是把所有对象对应匹配的方法都 mock 了（MockingClass），另一种是只 mock 特定对象的方法（MockingInstnace）

我们先声明一个测试类，方便后续 demo 验证

```kotlin
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
```

#### MockingClass

在 expectation 的构造参数中，传入 class，则表明所有该 class 的对象，在 expectation 中声明的方法都会被mocked
```kotlin
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
```

#### MockingInstance

在 expectation 的构造参数中，传入对象，则表明把该对象在 expectation 的方法给 mocked，不影响该类的其他实例
```kotlin
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
        
        //static 方法的 mock 方式，详细说明参见 static function mock
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
```

### static function mock
kotlin class 的 companion mock 和 java 的 static mock 有些不同，我们需要 mock 的是 Companion 这个 class

java 版
```java
        new Expectations(collaborator) {{
            collaborator.getValue(); result = 123;
            collaborator.simpleOperation(1, "", null); result = false;

            // Static methods can be dynamically mocked too.
            Collaborator.doSomething(anyBoolean, "test");
        }};
```

Kotlin 版
```kotlin
        val collaborator = Collaborator(2)

        object : Expectations(collaborator) {
            init {
                collaborator.value
                result = 123
                collaborator.simpleOperation(1, "", null)
                result = false
            }
        }

        object : Expectations(Collaborator.Companion) {
            init {
                // Static methods can be dynamically mocked too.
                Collaborator.doSomething(anyBoolean, "test")
            }
        }
```

即使加了 JvmStatic 也不行，因为在 kotlin 中，还是会在内部初始化一个 companion object，然后实现放在 companion object 中。代码会类似这样：
```java
      @JvmStatic
      public static final void doSomething(boolean b, @NotNull String s) {
         Intrinsics.checkParameterIsNotNull(s, "s");
         Companion.doSomething(b, s);
      }
      
      public static final class Companion {
         @JvmStatic
         public final void doSomething(boolean b, @NotNull String s) {
            Intrinsics.checkParameterIsNotNull(s, "s");
            throw (Throwable)(new IllegalStateException());
         }

         private Companion() {
         }

         // $FF: synthetic method
         public Companion(DefaultConstructorMarker $constructor_marker) {
            this();
         }
      }
```
所以我们的 PartialMock 对象，需要从 colloborator 改为 colloborator.Companion

如果 Colloborator 是一个 java class，则 static 方法还是我们熟知的 static 方式实现，因此 mock 还是按照普通的方式声明：

```kotlin
        val collaborator = Collaborator(2)

        object : Expectations(collaborator) {
            init {
                collaborator.value
                result = 123
                collaborator.simpleOperation(1, "", null)
                result = false
                // Static methods can be dynamically mocked too.
                Collaborator.doSomething(anyBoolean, "test")
            }
        }
```

## Verification

用于验证 mock 对象是否调用了特定的方法。只能是 mock 对象才能使用。一般是用来验证那些无返回的或者逻辑相对复杂的方法，通过是否调用某个方法来验证测试是否通过。
使用方式和 expectation 类似，只是需要放到方法调用之后。

Verification 有两类，一类是验证顺序的（VerificationInOrder, FullVerificationInOrder），另一类是不验证顺序的（Verification，FullVerification）。其中Full 开头的方法，验证方式很严格，必须所有的调用顺序都匹配上才行，少写一个都不可以，所以一般很少用到，因为通常我们只需要关注关键方法是否被调用到就好了

以下例子包含了 Verification，VerificationInOrder, FullVerificationInOrder 三种用法
```kotlin
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
```

### 注意事项

1. verification 的 block 里，填写的是非 mocked 对象时，测试实际上是通过的，因为是非 mocked 对象，不记录在验证的 record 里，相当于写了一个空的 block。所以在编写 verification 时，先故意写一个错误的验证（比如加 times = 100），如果验证通过，说明 verification 没有写对。
2. Fake对象是不能拿来 verify 的，想要 verify，只能通过 mocked 声明或者 partial mock

## 小结

1. 可以利用 expectation 来实现 mock 方法，重新定义返回值
2. partial mock 可以支持对一个 class 或者一个实例进行部分 mock，没有 mock 的部分仍然会调用原有实现
3. 想要在 expectation 中 mock 方法，对应的对象必须是 mock 的，或者是个 static 方法
4. static 方法在 kotlin 中，实际上要改为 mock Companion 这个class
5. Verification 有两类，一类是只验证方法是否被调用，另一类是较为严格，要验证方法的调用顺序
6. Verification 的 block 里必须是 mocked 对象，非 mocked 对象不生效

# Faking

顾名思义，欺骗，就是提供一个假的实现，其实和 partial mock 作用有些类似，但是和 mocked 不同的是，fake 对象不会被 record，也就是在 verification 里是无法使用的。一般来说，fake class 多用于第三方库，android 的 class等不需要进行测试的对象，是可复用的。因为和 mocking 是类似的，所以需要在前期和团队声明好，fake 应当仅用于external library，避免和 mock 用法混淆。

fake 可以用于@before，@beforeClass，属于在 test 初始化的一部分，而 mock 是跟随 test case 的，我们也可以把他们通过生命周期分隔开

```kotlin
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
```

## 小结
1. Fake 一般用于 external library，相当于可复用的 mock。
2. 我们定义 Fake 是在 test 初始化（@before, @beforeClass）时使用，和 mock 区别开来
3. Fake 实际上没有 record，因此不支持 verify
4. Fake 一个 static 方法时，和 mock 类似，需要看对应的 class 是 kotlin class 还是 java class，分别对应不同的方式。

# 一些注意事项
1. 不要忘记在 class 前加上@RunWith(JMockit:class)，否则执行 testcase 的引擎不正确，导致各种 mock 无效

[demo 源码地址](https://github.com/luhui/JMockitSampleInKotlin)
