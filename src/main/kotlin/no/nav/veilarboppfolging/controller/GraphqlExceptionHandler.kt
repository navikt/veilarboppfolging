package no.nav.veilarboppfolging.controller

import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolver
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

class PoaoTilgangError: RuntimeException {
    constructor(message: String): super(message)
}

class FantIkkeAktorIdForFnrError: RuntimeException {
    constructor(message: String): super(message)
}

@Component
class GraphqlExceptionHandler: DataFetcherExceptionResolver {
    override fun resolveException(ex: Throwable, env: DataFetchingEnvironment): Mono<List<GraphQLError>> {
        return Mono.just(listOf(
            GraphqlErrorBuilder.newError(env)
                .message(ex.message)
                .errorType(ErrorType.DataFetchingException)
                .build()
        ))
//        return when (ex) {
//            is PoaoTilgangError -> GraphqlErrorBuilder.newError(env)
//                .message(ex.message)
//                .errorType(ErrorType.DataFetchingException)
//                .build()
//            is ResponseStatusException -> GraphqlErrorBuilder.newError(env)
//                .message(ex.reason)
//                .errorType(ErrorType.valueOf(ex.statusCode.value().toString()))
//                .build()
//            else -> null
//        }
    }
}