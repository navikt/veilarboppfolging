package no.nav.veilarboppfolging.controller

import graphql.ErrorClassification
import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import no.nav.veilarboppfolging.ForbiddenException
import no.nav.veilarboppfolging.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.graphql.execution.DataFetcherExceptionResolver
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

abstract class GraphqlError: RuntimeException, ErrorClassification {
    abstract val errorType: ErrorType
    constructor(): super("Graphql error")
    abstract override fun toString(): String
}

class PoaoTilgangError(val ex: Throwable) : GraphqlError() {
    override val errorType: ErrorType = ErrorType.DataFetchingException
    override fun toString() = "Kunne ikke hente tilgangs-informasjon om bruker"
}

class FantIkkeAktorIdForFnrError: GraphqlError() {
    override val errorType: ErrorType = ErrorType.DataFetchingException
    override fun toString() = "Fant ikke bruker i Persondataløsningen"
}

class InternFeil(val errorMessage: String): GraphqlError() {
    override val errorType: ErrorType = ErrorType.ValidationError
    override fun toString() = errorMessage
}

@Component
class GraphqlExceptionHandler: DataFetcherExceptionResolver {
    private val logger = LoggerFactory.getLogger(GraphqlExceptionHandler::class.java)

    override fun resolveException(ex: Throwable, env: DataFetchingEnvironment): Mono<List<GraphQLError>> {
        logger.error("Error in graphql: ${ex.message}", ex)
        return when (ex) {
            is ForbiddenException -> GraphqlErrorBuilder.newError(env)
                .message(ex.message)
                .errorType(ErrorType.ValidationError)
                .build()
            is UnauthorizedException -> GraphqlErrorBuilder.newError(env)
                .message(ex.message)
                .errorType(ErrorType.ValidationError)
                .build()
            is GraphqlError -> GraphqlErrorBuilder.newError(env)
                .message(ex.toString())
                .errorType(ex.errorType)
                .build()
            else -> GraphqlErrorBuilder.newError(env)
                .message(ex.message)
                .errorType(ErrorType.DataFetchingException)
                .build()
        }.let {
            if (it != null) Mono.just(listOf(it))
            else Mono.empty()
        }
    }
}