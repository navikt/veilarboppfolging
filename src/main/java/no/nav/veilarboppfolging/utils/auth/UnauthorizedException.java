package no.nav.veilarboppfolging.utils.auth;

public class UnauthorizedException extends RuntimeException {
    UnauthorizedException(String message) {
        super(message);
    }
}
