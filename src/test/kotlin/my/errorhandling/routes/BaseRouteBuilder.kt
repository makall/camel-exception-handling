package my.errorhandling.routes

import org.apache.camel.ErrorHandlerFactory
import org.apache.camel.builder.RouteBuilder

class BaseRouteBuilder(
    val routeId: String,
    val nextRouteUri: String,
    val errorHandler: ErrorHandlerFactory,
    val handledException: Boolean,
) : RouteBuilder() {

    override fun configure() {
        onException()
            .handled(handledException)
            .log("onException")
            .to("mock:$routeId-onException")

        from("direct:$routeId")
            .routeId(routeId)
            .errorHandler(errorHandler)
            .doTry()
            .log("onTry")
            .to("mock:$routeId-onTry")
            .doCatch(Throwable::class.java)
            .log("onCatch")
            .to("mock:$routeId-onCatch")
            .end()
            .log("onNext")
            .to(nextRouteUri)
    }
}