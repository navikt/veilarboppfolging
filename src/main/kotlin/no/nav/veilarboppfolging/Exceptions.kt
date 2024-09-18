package no.nav.veilarboppfolging

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import java.lang.RuntimeException

private val logger = LoggerFactory.getLogger(VeilarboppfolgingException::class.java)
sealed class VeilarboppfolgingException(message: String) : RuntimeException(message) {
    open fun log() {
        logger.error("Error: ${this.message}", this)
    }
}

class UnauthorizedException(message: String) : VeilarboppfolgingException(message)
class ForbiddenException(message: String) : VeilarboppfolgingException(message)
class NotFoundException(message: String) : VeilarboppfolgingException(message)
class InternalServerError(message: String) : VeilarboppfolgingException(message)
class BadRequestException(message: String) : VeilarboppfolgingException(message)
class FantIkkeBrukerIArenaException() : VeilarboppfolgingException("Fant ikke bruker i arena") {
    override fun log() {
        logger.warn("Fant ikke oppfolgingsbruker i arena")
    }
}

@ControllerAdvice
class DefaultExceptionHandler {

    private val logger = LoggerFactory.getLogger(DefaultExceptionHandler::class.java)

    @ExceptionHandler(value = [NotImplementedError::class])
    fun onNotImplemented(ex: NotImplementedError, response: HttpServletResponse): Unit =
        response.sendError(HttpStatus.NOT_IMPLEMENTED.value())

    @ExceptionHandler(value = [ResponseStatusException::class])
    fun onResponseStatusException(ex: ResponseStatusException, response: HttpServletResponse): Unit =
        response.sendError(ex.statusCode.value(), ex.message)

    @ExceptionHandler(value = [VeilarboppfolgingException::class])
    fun onUnauthorized(ex: VeilarboppfolgingException, response: HttpServletResponse) {
        ex.log()
        when(ex) {
            is BadRequestException -> response.sendError(HttpStatus.BAD_REQUEST.value(), ex.message)
            is ForbiddenException -> response.sendError(HttpStatus.FORBIDDEN.value(), ex.message)
            is InternalServerError -> response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message)
            is NotFoundException -> response.sendError(HttpStatus.NOT_FOUND.value(), ex.message)
            is FantIkkeBrukerIArenaException -> response.sendError(HttpStatus.NOT_FOUND.value(), ex.message)
            is UnauthorizedException -> response.sendError(HttpStatus.UNAUTHORIZED.value(), ex.message)
        }
    }

}
