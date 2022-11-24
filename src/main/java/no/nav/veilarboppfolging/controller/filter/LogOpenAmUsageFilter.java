package no.nav.veilarboppfolging.controller.filter;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.utils.CookieUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.common.auth.Constants.OPEN_AM_ID_TOKEN_COOKIE_NAME;

@Slf4j
public class LogOpenAmUsageFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        Optional<String> tokenOptional = CookieUtils.getCookieValue(OPEN_AM_ID_TOKEN_COOKIE_NAME, request);

        if (tokenOptional.isPresent()) {
            try {
                JWT jwtToken = JWTParser.parse(tokenOptional.get());
                List<String> tokenAudiences = jwtToken.getJWTClaimsSet().getAudience();
                log.info("OpenAM call fra: " + tokenAudiences.stream().collect(Collectors.joining(", ")));
            } catch (ParseException e) {
            }
        }
    }
}
