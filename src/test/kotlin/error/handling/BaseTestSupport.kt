package error.handling

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.camel.Exchange
import org.apache.camel.builder.NoErrorHandlerBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.support.DefaultExchange
import org.apache.camel.test.junit5.CamelTestSupport
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.io.OutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseTestSupport : CamelTestSupport() {

    private lateinit var exceptionThrowerLabel: String
    private lateinit var exceptionCatcherLabel: String
    private lateinit var reportOutput: OutputStream

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

    @BeforeAll
    fun beforeAll() {

        assertEquals(expectedTestClassName, javaClass.simpleName)

        reportOutput = FileOutputStream("build/reports/tests/$expectedTestClassName.csv").apply {
            write(" $expectedTestClassName\n".toByteArray())
            write(" parent with ; ; child with ; ; exception ; ; result\n".toByteArray())
            write(" error handler ; handled exception ; error handler ; handled exception ;".toByteArray())
            write(" thrown at ; caught at ; was caught ; with failure ; with uncaught exception; test\n".toByteArray())
        }
    }

    @AfterAll
    fun afterAll() {
        reportOutput.apply {
            write(";;\n".toByteArray())
            close()
        }
    }

    @BeforeEach
    fun beforeEach() {

        appender.list.clear()
        resetMocks()

        exceptionThrowerLabel = ""
        exceptionCatcherLabel = ""
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
            exceptionCatcherLabel = "$routeId onCatch"
            setExpectedCount("onCatch", 1)
        }

        fun onException(routeId: String = this.routeId) = apply {
            exceptionCatcherLabel = "$routeId onException"
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

        private val expectedTestMethodName by lazy {
            StringBuilder()
                .apply {
                    val throwerEqualHandler =
                        exceptionThrowerLabel.let { it.isNotEmpty() && it == exceptionCatcherLabel }
                    val hasCatcher = exceptionCatcherLabel.isNotBlank() && !throwerEqualHandler
                    val hasThrower = exceptionThrowerLabel.isNotBlank()
                    smartAppend(!hasThrower, "should be successful if no exception is thrown")
                    smartAppend(hasThrower, "when having an exception in the $exceptionThrowerLabel,")
                    smartAppend(hasCatcher, "the $exceptionCatcherLabel will catch it")
                    smartAppend(hasCatcher && hasFailed, "and")
                    smartAppend(hasFailed, "camel will fail")
                    smartAppend(hasUnhandledException, "with unhandled exception")
                    smartAppend(hasFailed && !hasExceptionCaught, "with no exception caught")
                }
                .toString()
        }

        private fun report() = StringBuilder()
            .apply {
                smartAppend(expectedTestClassName.contains("HandlerOnBoth|HandlerOnParent".toRegex()), "x ")
                smartAppend(expectedTestClassName.contains("SingleRouteWithErrorHandler"), "x ")
                append(";")
                smartAppend(expectedTestClassName.contains("ExceptionOnBoth|ExceptionOnParent".toRegex()), "x ")
                append(";")
                smartAppend(expectedTestClassName.contains("HandlerOnBoth|HandlerOnChild".toRegex()), "x ")
                smartAppend(expectedTestClassName.contains("Single.*HandledException"), "x ")
                append(";")
                smartAppend(expectedTestClassName.contains("ExceptionOnBoth|ExceptionOnChild".toRegex()), "x ")
                append("; $exceptionThrowerLabel ; $exceptionCatcherLabel ;")
                smartAppend(hasExceptionCaught, "x ")
                append(";")
                smartAppend(hasFailed, "x ")
                append(";")
                smartAppend(hasUnhandledException, "x ")
                append("; $expectedTestMethodName \n")
            }
            .toString()
            .toByteArray()
            .let { reportOutput.write(it) }

        fun withFailure() = apply { hasFailed = true }
        fun withExceptionCaught() = apply { hasExceptionCaught = true }
        fun withUnhandledException() = apply { hasUnhandledException = true }

        fun assert() {

            val methodName = Thread.currentThread().stackTrace[2].methodName
            val exchange = template.send("direct:$routeId", DefaultExchange(context))

            assertEquals(expectedTestMethodName, methodName)

            assertMockEndpointsSatisfied()
            assertEquals(hasFailed, exchange.hasFailed())
            assertEquals(hasExceptionCaught, exchange.hasExceptionCaught())
            assertEquals(hasUnhandledException, unhandledExceptionCaught())

            report()
        }
    }
}