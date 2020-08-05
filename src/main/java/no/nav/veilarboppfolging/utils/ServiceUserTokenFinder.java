package no.nav.veilarboppfolging.utils;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.utils.TokenFinder;
import no.nav.common.auth.utils.TokenUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Optional;

@Slf4j
public class ServiceUserTokenFinder implements TokenFinder {

    @Override
    public Optional<String> findToken(HttpServletRequest request) {
        Optional<String> maybeToken = TokenUtils.getTokenFromHeader(request);

        if (maybeToken.isPresent()) {
            try {
                JWT jwt = JWTParser.parse(maybeToken.get());

                if (jwt.getJWTClaimsSet().getSubject().startsWith("srv")) {
                    return maybeToken;
                }
            } catch (ParseException e) {
                log.error("Failed to parse token", e);
            }
        }

        return Optional.empty();
    }

}
