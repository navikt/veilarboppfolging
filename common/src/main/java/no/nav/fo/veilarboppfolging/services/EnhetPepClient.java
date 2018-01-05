package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.lang.String.format;
import static no.nav.fo.veilarboppfolging.config.PepConfig.DOMAIN_VEILARB;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;

@Component
public class EnhetPepClient {

    @Inject
    private Pep pep;

    @SneakyThrows
    public void sjekkTilgang(String enhet) {
        BiasedDecisionResponse response = pep.harTilgang(pep.nyRequest()
                .withResourceType(ResourceType.Enhet)
                .withDomain(DOMAIN_VEILARB)
                .withEnhet(enhet));
        if (response.getBiasedDecision() != Permit) {
            throw new IngenTilgang(format("Veileder har ikke tilgang til enhet '%s'", enhet));
        }
    }

}
