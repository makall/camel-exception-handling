package my.errorhandling.routes

import org.apache.camel.builder.NoErrorHandlerBuilder
import org.junit.jupiter.api.Test

class SingleRouteWithoutErrorHandlerAndNoHandledException : BaseTestSupport() {

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
    fun `when having an exception on doTry, the exception will be handled by doCatch`() {

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
    fun `when having an exception on onException, the route will fail`() {

        WhenAnExceptionIsThrown(route)
            .onNext()
            .onException()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onNext()

        AndCompletionIsExpected(route)
            .withFailure()
            .assert()
    }

    @Test
    fun `when having an exception on doNext, the route will fail`() {

        WhenAnExceptionIsThrown(route)
            .onNext()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onNext()

        AndCompletionIsExpected(route)
            .withFailure()
            .assert()
    }

    override fun createRouteBuilder() =
        BaseRouteBuilder(route, lastMockUri, NoErrorHandlerBuilder(), false)
}

