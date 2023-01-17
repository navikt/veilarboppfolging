package no.nav.veilarboppfolging.utils.auth;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AuthorizationAnnotationHandler {

    private final AuthService authService;

    private static final List<Class<? extends Annotation>> SUPPORTED_ANNOTATIONS = List.of(
            AuthorizeFnr.class,
            AuthorizeAktorId.class
    );

    private void authorizeRequest(Annotation annotation, HttpServletRequest request) {
        var idToken = authService.getInnloggetBrukerToken();
        if (idToken == null || idToken.isEmpty()) {
            throw new UnauthorizedException("Missing token");
        }
        if (annotation instanceof AuthorizeFnr) {
            var fnr = Fnr.of(getFnr(request));
            var allowlist = ((AuthorizeFnr) annotation).allowlist();
            authorizeFnr(fnr, allowlist);
        } else if (annotation instanceof AuthorizeAktorId) {
            var allowlist = ((AuthorizeAktorId) annotation).allowlist();
            var aktorId = AktorId.of(getAktorId(request));
            authorizeAktorId(aktorId, allowlist);
        }
    }

    private void authorizeFnr(Fnr fnr, String[] allowlist) {
        if (authService.erSystemBrukerFraAzureAd()) {
            authService.sjekkAtApplikasjonErIAllowList(allowlist);
        } else {
            authService.sjekkLesetilgangMedFnr(fnr);
        }
    }

    private void authorizeAktorId(AktorId aktorId, String[] allowlist) {
        if (authService.erInternBruker()) {
            authService.sjekkLesetilgangMedAktorId(aktorId);
        } else if (authService.erSystemBrukerFraAzureAd()) {
            authService.sjekkAtApplikasjonErIAllowList(allowlist);
        } else if (authService.erEksternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Eksternbruker ikke tillatt");
        }
    }

    public void doAuthorizationCheckIfTagged(Method handlerMethod, HttpServletRequest request) {
        Optional.ofNullable(getAnnotation(handlerMethod, SUPPORTED_ANNOTATIONS))
            // Skip if not tagged
            .ifPresent((Annotation annotation) -> {
                authorizeRequest(annotation, request);
            });
    }

    protected Annotation getAnnotation(Method method, List<Class<? extends Annotation>> types) {
        return Optional.ofNullable(findAnnotation(types, method.getAnnotations()))
                .orElseGet(() -> findAnnotation(types, method.getDeclaringClass().getAnnotations()));
    }

    private static Annotation findAnnotation(List<Class<? extends Annotation>> types, Annotation... annotations) {
        return Arrays.stream(annotations)
                .filter(a -> types.contains(a.annotationType()))
                .findFirst()
                .orElse(null);
    }
    private String getFnr(HttpServletRequest request) {
        /* Get fnr from headers instead of query when supported by clients */
        return request.getParameter("fnr");
    }
    private String getAktorId(HttpServletRequest request) {
        return request.getParameter("aktorId");
    }

}
