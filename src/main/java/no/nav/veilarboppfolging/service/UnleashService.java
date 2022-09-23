package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class

UnleashService {
    private final static String OPPDATER_OPPFOLGING_KAFKA = "veilarboppfolging.oppdater_oppfolging_kafka";

    private final static String IKKE_OPPDATER_OPPFOLGING_MED_SIDEEFFEKT = "veilarboppfolging.ikke_oppdater_oppfolging_med_sideeffekt";
    private static final String LAGRE_VEILEDER_SOM_HAR_UTFORT_TILORDNING = "veilarboppfolging.lagre_veileder_som_har_utfort_tilordning";

    private final UnleashClient unleashClient;

    public boolean skalOppdaterOppfolgingMedKafka() {
        return unleashClient.isEnabled(OPPDATER_OPPFOLGING_KAFKA);
    }

    public boolean skalIkkeOppdatereMedSideeffekt() {
        return unleashClient.isEnabled(IKKE_OPPDATER_OPPFOLGING_MED_SIDEEFFEKT);
    }

    public boolean skalLagreHvilkenVeilederSomHarUtfortTilordning() {
        return unleashClient.isEnabled(LAGRE_VEILEDER_SOM_HAR_UTFORT_TILORDNING);
    }
}
