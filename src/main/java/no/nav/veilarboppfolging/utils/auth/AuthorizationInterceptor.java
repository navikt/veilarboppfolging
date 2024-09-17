package no.nav.veilarboppfolging.utils.auth;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.InternalServerError;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {
    private final AuthorizationAnnotationHandler annotationHandler;

    public AuthorizationInterceptor(AuthService authService) {
        this.annotationHandler = new AuthorizationAnnotationHandler(authService);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            try {
                annotationHandler.doAuthorizationCheckIfTagged(((HandlerMethod) handler).getMethod(), request);
            } catch (Exception e) {
                // Catch all exception except status-exceptions
                if (e instanceof ResponseStatusException) {
                    return true;
                }
                log.error("Failed to process annotation", e);
                throw new InternalServerError("Failed to process annotation");
            }
        }
        return true;
    }

}
