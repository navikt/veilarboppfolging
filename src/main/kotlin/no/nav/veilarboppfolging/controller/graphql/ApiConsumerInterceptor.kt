package no.nav.veilarboppfolging.controller.graphql

import no.nav.veilarboppfolging.service.AuthService
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ApiConsumerInterceptor(
    private val authService: AuthService,
): WebGraphQlInterceptor {

    override fun intercept(request: WebGraphQlRequest, chain: WebGraphQlInterceptor.Chain): Mono<WebGraphQlResponse?> {
        val apiConsumer = authService.hentApplikasjonFraContext()

        request.configureExecutionInput { input, builder ->
            builder.graphQLContext {
                it.put("apiConsumer", apiConsumer)
            }.build()
        }

        return chain.next(request)
    }

}
