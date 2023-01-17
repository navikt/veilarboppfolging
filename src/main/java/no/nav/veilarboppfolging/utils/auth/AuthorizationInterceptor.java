package no.nav.veilarboppfolging.utils.auth;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return true;
    }

}
