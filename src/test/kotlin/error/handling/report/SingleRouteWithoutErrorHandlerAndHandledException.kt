package error.handling.report

import org.apache.camel.builder.NoErrorHandlerBuilder
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

class SingleRouteWithoutErrorHandlerAndHandledException : BaseTestSupport() {

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
    fun `when having an exception in the route onException, camel will fail with no exception caught`() {

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
    @Order(4)
    fun `when having an exception in the route onNext, camel will fail with no exception caught`() {

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
        BaseRouteBuilder(route, lastMockUri(route), NoErrorHandlerBuilder(), true)
}

