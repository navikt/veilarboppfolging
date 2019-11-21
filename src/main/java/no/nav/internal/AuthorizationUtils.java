package no.nav.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

import static java.lang.System.getProperty;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;

@Slf4j
class AuthorizationUtils {

    private static final String AUTHENTICATE = "WWW-Authenticate";
    private static final String BASIC_REALM = "BASIC realm=\"srvveilarbportefolje\"";
    private static final String AUTHORIZATION = "Authorization";
    private static final Base64.Decoder decoder = Base64.getDecoder();

    static boolean isBasicAuthAuthorized(HttpServletRequest request) {
        String auth = request.getHeader(AUTHORIZATION);
        if (Objects.isNull(auth) || !auth.toLowerCase().startsWith("basic ")) {
            return false;
        }

        String basicAuth = auth.substring(6);
        String basicAuthDecoded = new String(decoder.decode(basicAuth));

        String username = basicAuthDecoded.split(":")[0].toLowerCase();
        String password = basicAuthDecoded.split(":")[1];
        String srvUsername = getProperty(SYSTEMUSER_USERNAME).toLowerCase();
        String srvPassword = getProperty(SYSTEMUSER_PASSWORD);

        return username.equals(srvUsername) && password.equals(srvPassword);
    }

    @SneakyThrows
    static void writeUnauthorized(HttpServletResponse resp) {
        resp.setHeader(AUTHENTICATE, BASIC_REALM);
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
