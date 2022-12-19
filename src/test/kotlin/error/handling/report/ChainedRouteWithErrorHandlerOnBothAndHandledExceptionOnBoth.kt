package error.handling.report

import org.apache.camel.builder.DefaultErrorHandlerBuilder
import org.junit.jupiter.api.Test

class ChainedRouteWithErrorHandlerOnBothAndHandledExceptionOnBoth : BaseTestSupport() {

    private val parent = "parent"
    private val child = "child"

    @Test
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
    fun `when having an exception in the child onCatch, the parent onException will catch it`() {

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
            .withExceptionCaught()
            .assert()
    }

    @Test
    fun `when having an exception in the child onException, camel will fail with unhandled exception`() {

        WhenAnExceptionIsThrown(child)
            .onNext()
            .onException()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onNext()
            .onException()

        AndCompletionIsExpected(parent)
            .withUnhandledException()
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    @Test
    fun `when having an exception in the child onNext, the child onException will catch it`() {

        WhenAnExceptionIsThrown(child)
            .onNext()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onNext()
            .onException()

        AndCompletionIsExpected(parent)
            .withExceptionCaught()
            .assert()
    }

    override fun createRouteBuilders() = arrayOf(
        BaseRouteBuilder(parent, "direct:$child", DefaultErrorHandlerBuilder().log(logger), true),
        BaseRouteBuilder(child, lastMockUri(child), DefaultErrorHandlerBuilder().log(logger), true)
    )
}

