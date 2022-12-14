package my.errorhandling.routes

import org.apache.camel.builder.NoErrorHandlerBuilder
import org.junit.jupiter.api.Test

class ChainedRouteWithErrorHandlerOnNoneAndHandledExceptionOnNone : BaseTestSupport() {

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
    fun `when having an exception on doTry, the onCatch will handle the exception`() {

        WhenAnExceptionIsThrown(child).onTry()

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
    fun `when having an exception on doCatch, the parentOnException() will catch the exception, but the route will fail with unhandled exceptions`() {

        WhenAnExceptionIsThrown(child)
            .onTry()
            .onCatch()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onCatch()

        AndCompletionIsExpected(parent)
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    @Test
    fun `when having an exception on onException, the route will fail with unhandled exceptions`() {

        WhenAnExceptionIsThrown(child)
            .onNext()
            .onException()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onNext()

        AndCompletionIsExpected(parent)
            .withFailure()
            .assert()
    }

    @Test
    fun `when having an exception on doNext, the exception will be handled by onException`() {

        WhenAnExceptionIsThrown(child)
            .onNext()

        ThenTheExpectedPathIs(parent)
            .onTry()
            .onNext(child)
            .onTry()
            .onNext()

        AndCompletionIsExpected(parent)
            .withFailure()
            .assert()
    }

    override fun createRouteBuilders() = arrayOf(
        BaseRouteBuilder(parent, "direct:$child", NoErrorHandlerBuilder(), false),
        BaseRouteBuilder(child, lastMockUri, NoErrorHandlerBuilder(), false)
    )
}

