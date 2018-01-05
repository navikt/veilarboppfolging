package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.lang.String.format;

@Component
public class EnhetPepClient {

    @Inject
    private Pep pep;

    @SneakyThrows
    public void sjekkTilgang(String enhet) {
        BiasedDecisionResponse response = pep.harTilgang(pep.nyRequest()
                .withResourceType(ResourceType.Enhet)
                .withDomain("veilarb")
                .withEnhet(enhet));
        if (response.getBiasedDecision() != Decision.Permit) {
            throw new IngenTilgang(format("Veileder har ikke tilgang til enhet '%s'", enhet));
        }
    }

}
