package error.handling.sandbox

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.camel.ExchangePropertyKey.EXCEPTION_CAUGHT
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.processor.FatalFallbackErrorHandler
import org.apache.camel.processor.errorhandler.DefaultErrorHandler
import org.apache.camel.support.DefaultExchange
import org.apache.camel.test.junit5.CamelTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class Sandbox : CamelTestSupport() {

    private val appender = ListAppender<ILoggingEvent>().apply {
        arrayOf(DefaultErrorHandler::class.java, FatalFallbackErrorHandler::class.java)
            .map { LoggerFactory.getLogger(it) as Logger }
            .forEach { it.addAppender(this) }
        start()
    }

    @Test
    fun `expected result` () {
        val exchange = template.send(Root.ROUTE_URI, DefaultExchange(context))
        assertTrue(exchange.isFailed)
        assertEquals(Leaf.ROUTE_ID, exchange.exception.message)
        assertEquals(exchange.exception, exchange.getProperty(EXCEPTION_CAUGHT))
        assertTrue(appender.list.any { it.message.contains("FatalFallbackErrorHandler") })
    }

    class Root : RouteBuilder() {

        override fun configure() {
            onException()
                .process { assert(false) { "Should no pass here" } }

            from(ROUTE_URI)
                .routeId(ROUTE_ID)
                .to(Trunk.ROUTE_URI)
                .throwException(RuntimeException(ROUTE_ID))
        }

        companion object {
            private const val ROUTE_ID = "root"
            const val ROUTE_URI = "direct:$ROUTE_ID"
        }
    }

    class Trunk : RouteBuilder() {

        override fun configure() {
            onException()
                .process { assert(false) { "Should no pass here" } }

            from(ROUTE_URI)
                .routeId(ROUTE_ID)
                .to(Leaf.ROUTE_URI)
                .throwException(RuntimeException(ROUTE_ID))
        }

        companion object {
            private const val ROUTE_ID = "trunk"
            const val ROUTE_URI = "direct:$ROUTE_ID"
        }
    }

    class Leaf : RouteBuilder() {
        override fun configure() {

            onException()
                // .handled(true)
                // .continued(true)
                .log("Ops!")

            errorHandler(
                defaultErrorHandler()
                    .logExhaustedMessageHistory(false)
                    .logStackTrace(false)
            )

            from(ROUTE_URI)
                .routeId(ROUTE_ID)
                .throwException(RuntimeException(ROUTE_ID))
        }

        companion object {
            internal const val ROUTE_ID = "leaf"
            const val ROUTE_URI = "direct:$ROUTE_ID"
        }
    }

    override fun createRouteBuilders() = arrayOf(Root(), Trunk(), Leaf())
}