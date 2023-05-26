package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnleashService {
    private final static String IKKE_OPPDATER_OPPFOLGING_MED_SIDEEFFEKT = "veilarboppfolging.ikke_oppdater_oppfolging_med_sideeffekt";
	private static final String POAO_TILGANG_ENABLED = "veilarboppfolging.poao-tilgang-enabled";

    private final UnleashClient unleashClient;

    public boolean skalIkkeOppdatereMedSideeffekt() {
        return unleashClient.isEnabled(IKKE_OPPDATER_OPPFOLGING_MED_SIDEEFFEKT);
    }

	public boolean skalBrukePoaoTilgang() {
		return unleashClient.isEnabled(POAO_TILGANG_ENABLED);
	}
}
