package my.errorhandling.routes

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.camel.Exchange
import org.apache.camel.builder.NoErrorHandlerBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.support.DefaultExchange
import org.apache.camel.test.junit5.CamelTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory

abstract class BaseTestSupport : CamelTestSupport() {

    protected var exceptionThrowerLabel = ""
    protected var exceptionHandlerLabel = ""

    protected val logger = LoggerFactory.getLogger(javaClass) as Logger

    private val appender = ListAppender<ILoggingEvent>().apply {
        val fatalErrorHandlerLogger = LoggerFactory.getLogger(FatalFallbackErrorHandler::class.java) as Logger
        fatalErrorHandlerLogger.addAppender(this)
        logger.addAppender(this)
        start()
    }

    private val expectedTestClassName by lazy {
        val builders = createRouteBuilders().filterIsInstance<BaseRouteBuilder>()
        StringBuilder()
            .apply {
                smartAppend(builders.size == 1, "Single", "Chained")
                when {
                    builders.all { it.errorHandler is NoErrorHandlerBuilder } -> "None"
                    !builders.any { it.errorHandler is NoErrorHandlerBuilder } -> "Both"
                    else -> builders.first { it.errorHandler !is NoErrorHandlerBuilder }.routeId
                }.let { append("RouteWithErrorHandlerOn${it.replaceFirstChar(Char::titlecase)}") }
                when {
                    builders.all { it.handledException } -> "Both"
                    !builders.any { it.handledException } -> "None"
                    else -> builders.first { !it.handledException }.routeId
                }.let { append("AndHandledExceptionOn${it.replaceFirstChar(Char::titlecase)}") }
            }
            .toString()
            .let {
                if (builders.size > 1) it
                else it
                    .replace("OnBoth", "")
                    .replace("WithErrorHandlerOnNone", "WithoutErrorHandler")
                    .replace("HandledExceptionOnNone", "NoHandledException")
            }
    }

    @BeforeEach
    fun beforeEach() {
        assertEquals(expectedTestClassName, javaClass.simpleName)
        appender.list.clear()
        resetMocks()
    }

    private fun Exchange.hasFailed() = (exception != null)
    private fun Exchange.hasExceptionCaught() = (getProperty(Exchange.EXCEPTION_CAUGHT) != null)

    private fun StringBuilder.smartAppend(conditional: Boolean, item: String, orElse: String = "") {
        if (isNotBlank() && (conditional || orElse.isNotBlank())) {
            append(" ")
        }
        if (conditional) append(item)
        else if (orElse.isNotBlank()) append(orElse)
    }

    private fun unhandledExceptionCaught() = appender.list.stream()
        .map { it.message }
        .anyMatch { it.contains("Failed delivery") || it.contains("Exception occurred") }

    private fun getMockUri(routeId: String, mockId: String): String = "mock:$routeId-$mockId"

    private fun getMock(routeId: String, mockId: String) = getMockEndpoint(getMockUri(routeId, mockId))

    protected fun lastMockUri(routeId: String) = "mock:$routeId-onNext"

    inner class WhenAnExceptionIsThrown(private val routeId: String) {

        private fun getMock(mockId: String) = getMock(routeId, mockId)
        private fun throwExceptionOn(mockId: String) = throwExceptionOn(getMock(mockId))

        private fun throwExceptionOn(mock: MockEndpoint) = apply {
            exceptionThrowerLabel = mock.name.replace("-", " ")
            mock.whenAnyExchangeReceived { throw RuntimeException() }
        }

        fun onTry() = throwExceptionOn("onTry")
        fun onCatch() = throwExceptionOn("onCatch")
        fun onException() = throwExceptionOn("onException")
        fun onNext() = throwExceptionOn(getMockEndpoint(lastMockUri(routeId)))
    }

    inner class ThenTheExpectedPathIs(private var routeId: String) {

        init {
            context.endpoints
                .filterIsInstance<MockEndpoint>()
                .forEach { it.expectedCount = 0 }
        }

        private fun setExpectedCount(mockId: String, count: Int) =
            setExpectedCount(routeId, mockId, count)

        private fun setExpectedCount(routeId: String, mockId: String, count: Int) =
            setExpectedCount(getMock(routeId, mockId), count)

        private fun setExpectedCount(mock: MockEndpoint, count: Int) =
            apply { mock.expectedCount = count }

        fun onTry() =
            setExpectedCount("onTry", 1)

        fun onCatch() = apply {
            exceptionHandlerLabel = "$routeId onCatch"
            setExpectedCount("onCatch", 1)
        }

        fun onException(routeId: String = this.routeId) = apply {
            exceptionHandlerLabel = "$routeId onException"
            setExpectedCount(routeId, "onException", 1)
        }

        fun onNext() =
            apply { getMockEndpoint(lastMockUri(routeId)).expectedCount = 1 }

        fun onNext(nextRouteId: String) =
            apply { routeId = nextRouteId }
    }

    inner class AndCompletionIsExpected(private val routeId: String) {

        private var hasFailed = false
        private var hasExceptionCaught = false
        private var hasUnhandledException = false

        private fun expectedTestMethodName() = StringBuilder()
            .apply {
                val throwerEqualHandler = exceptionThrowerLabel.let { it.isNotEmpty() && it == exceptionHandlerLabel }
                val hasHandler = exceptionHandlerLabel.isNotBlank() && !throwerEqualHandler
                val hasThrower = exceptionThrowerLabel.isNotBlank()
                smartAppend(!hasThrower, "should be successful if no exception is thrown")
                smartAppend(hasThrower, "when having an exception in the $exceptionThrowerLabel,")
                smartAppend(hasHandler, "the $exceptionHandlerLabel will catch it")
                smartAppend(hasHandler && hasFailed, "and")
                smartAppend(hasFailed, "camel will fail")
                smartAppend(hasUnhandledException, "with unhandled exception")
                smartAppend(hasFailed && !hasExceptionCaught, "with no exception caught")
            }
            .toString()

        fun withFailure() = apply { hasFailed = true }
        fun withExceptionCaught() = apply { hasExceptionCaught = true }
        fun withUnhandledException() = apply { hasUnhandledException = true }

        fun assert() {

            val testMethodName = Thread.currentThread().stackTrace[2].methodName
            val exchange = template.send("direct:$routeId", DefaultExchange(context))

            assertEquals(expectedTestMethodName(), testMethodName)

            assertMockEndpointsSatisfied()
            assertEquals(hasFailed, exchange.hasFailed())
            assertEquals(hasExceptionCaught, exchange.hasExceptionCaught())
            assertEquals(hasUnhandledException, unhandledExceptionCaught())
        }
    }
}