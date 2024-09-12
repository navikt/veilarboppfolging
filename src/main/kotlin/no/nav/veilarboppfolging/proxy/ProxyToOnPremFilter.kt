package no.nav.veilarboppfolging.proxy

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.net.URI
import java.util.function.Function
import java.util.function.Supplier

import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.https
import org.springframework.web.servlet.function.RequestPredicates.path

@Profile("!local")
@Service
@RequiredArgsConstructor
@Slf4j
class ProxyToOnPremFilter(
    val proxyToOnPremTokenProvider: ProxyToOnPremTokenProvider,
    @Value("\${veilarboppfolging-fss.url}")
    val veilarbdialogFssUrl: String
) {
    val logger = LoggerFactory.getLogger(ProxyToOnPremFilter::class.java)

    private fun oboExchange(getToken: Supplier<String>): Function<ServerRequest, ServerRequest> {
        return Function<ServerRequest, ServerRequest> { request: ServerRequest ->
            logger.info("Gateway obo $request")
            val requestBuilder = ServerRequest.from(request)
            val oldAuthHeaderValues =
                request.headers().header(HttpHeaders.AUTHORIZATION)
            requestBuilder.headers { headers: HttpHeaders ->
                headers.replace(
                    HttpHeaders.AUTHORIZATION,
                    oldAuthHeaderValues,
                    listOf("Bearer " + getToken.get())
                )
            }
            requestBuilder.build()
        }
    }

    @Bean
    @Order(-1)
    @ConditionalOnProperty(name = ["spring.cloud.gateway.mvc.enabled"], havingValue = "true")
    fun getRoute(): RouterFunction<ServerResponse> {
        val sendToOnPrem = https(URI.create(veilarbdialogFssUrl))
        return route()
            .route(
                path("/internal/isAlive")
                    .or(path("/internal/isReady"))
                    .or(path("/internal/selftest"))
                    .negate(), sendToOnPrem
            )
            .before(oboExchange { proxyToOnPremTokenProvider.getProxyToken() })
            .onError(
                { error: Throwable? ->
                    logger.error("Proxy error", error)
                    true
                },
                ({ error: Throwable, request: ServerRequest? ->
                    ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.stackTrace.toString())
                })
            )
            .build()
    }
}