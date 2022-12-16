package error.handling

import org.apache.camel.builder.DefaultErrorHandlerBuilder
import org.junit.jupiter.api.Test

class SingleRouteWithErrorHandlerAndNoHandledException : BaseTestSupport() {

    private val route = "route"

    @Test
    fun `should be successful if no exception is thrown`() {

        ThenTheExpectedPathIs(route)
            .onTry()
            .onNext()

        AndCompletionIsExpected(route)
            .assert()
    }

    @Test
    fun `when having an exception in the route onTry, the route onCatch will catch it`() {

        WhenAnExceptionIsThrown(route)
            .onTry()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onCatch()
            .onNext()

        AndCompletionIsExpected(route)
            .withExceptionCaught()
            .assert()
    }

    @Test
    fun `when having an exception in the route onCatch, camel will fail`() {

        WhenAnExceptionIsThrown(route)
            .onTry()
            .onCatch()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onCatch()

        AndCompletionIsExpected(route)
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    @Test
    fun `when having an exception in the route onException, camel will fail with unhandled exception`() {

        WhenAnExceptionIsThrown(route)
            .onNext()
            .onException()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onNext()
            .onException()

        AndCompletionIsExpected(route)
            .withUnhandledException()
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    @Test
    fun `when having an exception in the route onNext, the route onException will catch it and camel will fail with unhandled exception`() {

        WhenAnExceptionIsThrown(route)
            .onNext()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onNext()
            .onException()

        AndCompletionIsExpected(route)
            .withUnhandledException()
            .withExceptionCaught()
            .withFailure()
            .assert()
    }

    override fun createRouteBuilder() =
        BaseRouteBuilder(route, lastMockUri(route), DefaultErrorHandlerBuilder().log(logger), false)
}

