package error.handling.report

import org.apache.camel.builder.DefaultErrorHandlerBuilder
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

class SingleRouteWithErrorHandlerAndHandledException : BaseTestSupport() {

    private val route = "route"

    @Test
    @Order(1)
    fun `should be successful if no exception is thrown`() {

        ThenTheExpectedPathIs(route)
            .onTry()
            .onNext()

        AndCompletionIsExpected(route)
            .assert()
    }

    @Test
    @Order(2)
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
    @Order(3)
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
    @Order(5)
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
    @Order(4)
    fun `when having an exception in the route onNext, the route onException will catch it`() {

        WhenAnExceptionIsThrown(route)
            .onNext()

        ThenTheExpectedPathIs(route)
            .onTry()
            .onNext()
            .onException()

        AndCompletionIsExpected(route)
            .withExceptionCaught()
            .assert()
    }

    override fun createRouteBuilder() =
        BaseRouteBuilder(route, lastMockUri(route), DefaultErrorHandlerBuilder().log(logger), true)
}

