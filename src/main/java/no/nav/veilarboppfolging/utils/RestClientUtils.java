package no.nav.veilarboppfolging.utils;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;

@Slf4j
public class RestClientUtils {

    public static String authHeaderMedInnloggetBruker() {
        return "Bearer " + SubjectHandler.getSsoToken().map(SsoToken::getToken).orElseThrow(() -> new RuntimeException("Fant ikke token til innlogget bruker"));
    }

}
