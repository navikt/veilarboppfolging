package no.nav.veilarboppfolging.utils;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;

@Slf4j
public class RestClientUtils {

    public static String authHeaderMedInnloggetBruker() {
        return "Bearer " + AuthContextHolder.getIdTokenString().orElseThrow(() -> new RuntimeException("Fant ikke token til innlogget bruker"));
    }

}
