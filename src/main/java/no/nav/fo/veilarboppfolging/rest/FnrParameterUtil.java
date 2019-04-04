package no.nav.fo.veilarboppfolging.rest;

import static no.nav.common.auth.SubjectHandler.getIdent;
import static no.nav.common.auth.SubjectHandler.getIdentType;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import no.nav.brukerdialog.security.domain.IdentType;

@Component
public class FnrParameterUtil {

    @Inject
    private Provider<HttpServletRequest> requestProvider;

    static boolean erEksternBruker() {
        return getIdentType()
                .map(identType -> IdentType.EksternBruker == identType)
                .orElse(false);
    }

    String getFnr() {
        if (erEksternBruker()) {
            return getIdent().orElseThrow(RuntimeException::new);
        }
        return Optional.ofNullable(requestProvider.get().getParameter("fnr")).orElseThrow(RuntimeException::new);
    }


}
