package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.domain.ResourceType;
import no.nav.sbl.dialogarena.common.abac.pep.domain.response.BiasedDecisionResponse;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.lang.String.format;
import static no.nav.fo.veilarboppfolging.config.PepConfig.DOMAIN_VEILARB;
import static no.nav.sbl.dialogarena.common.abac.pep.domain.response.Decision.Permit;

@Slf4j
@Component
public class EnhetPepClient {

    @Inject
    private Pep pep;

    @SneakyThrows
    public void sjekkTilgang(String enhet) {
        if (getBiasedDecisionResponse(enhet).getBiasedDecision() != Permit) {
            throw new IngenTilgang(format("Veileder har ikke tilgang til enhet '%s'", enhet));
        }
    }

    public boolean harTilgang(String enhet) {
        try {
            return getBiasedDecisionResponse(enhet).getBiasedDecision() == Permit;
        } catch (PepException e) {
            log.error(format("Tilgangsjekk mot enhet '%s' feilet.", enhet), e);
            return false;
        }
    }

    private BiasedDecisionResponse getBiasedDecisionResponse(String enhet) throws PepException {
        return pep.harTilgang(pep.nyRequest()
                .withResourceType(ResourceType.Enhet)
                .withDomain(DOMAIN_VEILARB)
                .withEnhet(enhet));
    }

}
