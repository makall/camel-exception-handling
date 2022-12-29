package error.handling.report

import org.apache.camel.builder.DefaultErrorHandlerBuilder
import org.apache.camel.builder.NoErrorHandlerBuilder
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

class ChainedRouteWithErrorHandlerOnParentAndHandledExceptionOnNone : BaseTestSupport() {

    private val parent = "parent"
    private val child = "child"

    @Test
    @Order(1)
    fun `should be successful if no exception is thrown`() {

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onNext()

        AndCompletionIsExpected(parent)
            .assert()
    }

    @Test
    @Order(2)
    fun `when having an exception in the child onTry, the child onCatch will catch it`() {

        WhenAnExceptionIsThrown(child)
            .onTry()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onCatch()
            .onNext()

        AndCompletionIsExpected(parent)
            .withExceptionCaught()
            .assert()
    }

    @Test
    @Order(3)
    fun `when having an exception in the child onCatch, the parent onException will catch it and camel will fail with unhandled exception`() {

        WhenAnExceptionIsThrown(child)
            .onTry()
            .onCatch()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onCatch()
            .onException(parent)

        AndCompletionIsExpected(parent)
            .withUnhandledException()
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    @Test
    @Order(5)
    fun `when having an exception in the child onException, the parent onException will catch it and camel will fail with unhandled exception`() {

        WhenAnExceptionIsThrown(child)
            .onNext()
            .onException()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onNext()
            .onException(parent)

        AndCompletionIsExpected(parent)
            .withUnhandledException()
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    @Test
    @Order(4)
    fun `when having an exception in the child onNext, the parent onException will catch it and camel will fail with unhandled exception`() {

        WhenAnExceptionIsThrown(child)
            .onNext()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onNext()
            .onException(parent)

        AndCompletionIsExpected(parent)
            .withUnhandledException()
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    override fun createRouteBuilders() = arrayOf(
        BaseRouteBuilder(parent, "direct:$child", DefaultErrorHandlerBuilder().log(logger), false),
        BaseRouteBuilder(child, lastMockUri(child), NoErrorHandlerBuilder(), false)
    )
}

