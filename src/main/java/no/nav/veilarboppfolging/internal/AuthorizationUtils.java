package no.nav.veilarboppfolging.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Objects;

@Slf4j
class AuthorizationUtils {

    private static final String AUTHENTICATE = "WWW-Authenticate";
    private static final String BASIC_REALM = "BASIC realm=\"srvveilarbportefolje\"";
    private static final String AUTHORIZATION = "Authorization";
    private static final Base64.Decoder decoder = Base64.getDecoder();

    static boolean isBasicAuthAuthorized(HttpServletRequest request, String expectedSystemUserName, String expectedSystemUserPassword) {
        String auth = request.getHeader(AUTHORIZATION);
        if (Objects.isNull(auth) || !auth.toLowerCase().startsWith("basic ")) {
            return false;
        }

        String basicAuth = auth.substring(6);
        String basicAuthDecoded = new String(decoder.decode(basicAuth));

        String username = basicAuthDecoded.split(":")[0].toLowerCase();
        String password = basicAuthDecoded.split(":")[1];

        return username.equals(expectedSystemUserName) && password.equals(expectedSystemUserPassword);
    }

    @SneakyThrows
    static void writeUnauthorized(HttpServletResponse resp) {
        resp.setHeader(AUTHENTICATE, BASIC_REALM);
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
