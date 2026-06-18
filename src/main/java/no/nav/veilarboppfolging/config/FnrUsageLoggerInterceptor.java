package no.nav.veilarboppfolging.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.nav.common.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component

public class FnrUsageLoggerInterceptor implements HandlerInterceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fnrPattern = ".*\\b\\d{11}\\b.*";

        boolean hasFnrInRequestURI = StringUtils.notNullOrEmpty(requestURI) && requestURI.matches(fnrPattern);
        boolean hasFnrInQueryString = StringUtils.notNullOrEmpty(queryString) && queryString.matches(fnrPattern);

        if (hasFnrInRequestURI || hasFnrInQueryString) {
            log.info("Konsument forespurte endepunkt som matcher fnr-regex.");
        }

        return true;
    }
}
