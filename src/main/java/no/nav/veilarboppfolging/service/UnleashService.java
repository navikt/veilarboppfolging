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
    private static final String BRUK_SISTE_ENDRING_DATO_I_ENDRING_PAA_OPPFOLGING_BRUKER = "veilarboppfolging.bruk_siste_endring_dato_i_endring_paa_oppfolging_bruker";

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

    public boolean skalBrukeSisteEndringDatoIEndringPaaOppfoelgingsBrukerV2() {
        return unleashClient.isEnabled(BRUK_SISTE_ENDRING_DATO_I_ENDRING_PAA_OPPFOLGING_BRUKER);
    }
}
