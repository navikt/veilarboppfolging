package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SubjectHandler;
import org.springframework.stereotype.Component;

@Component
public class AutorisasjonService {

    public void skalVæreVeileder() {
        skalVære(IdentType.InternBruker);
    }

    public void skalVære(IdentType forventetIdentType) {
        IdentType identType = SubjectHandler.getIdentType().orElse(null);
        if (identType != forventetIdentType) {
            throw new IngenTilgang(String.format("%s != %s", identType, forventetIdentType));
        }
    }

}
