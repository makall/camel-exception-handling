package my.errorhandling.routes

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
    fun `when having an exception on doTry, the onCatch will be reached`() {

        WhenAnExceptionIsThrown(route).onTry()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onCatch()
            .onNext()

        AndCompletionIsExpected(route)
            .withExceptionCaught()
            .assert()
    }

    @Test
    fun `when having an exception on doCatch, the route will fail`() {

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
    fun `when having an exception on onException, the route will fail with unhandled exceptions`() {

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
    fun `when having an exception on doNext, the exception will be handled by onException, but the route will fail with unhandled exceptions`() {

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
        BaseRouteBuilder(route, lastMockUri, DefaultErrorHandlerBuilder().log(logger), false)
}

