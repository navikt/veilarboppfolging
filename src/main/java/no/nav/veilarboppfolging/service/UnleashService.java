package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnleashService {
    private final static String OPPDATER_OPPFOLGING_KAFKA = "veilarboppfolging.oppdater_oppfolging_kafka";

    private final static String IKKE_OPPDATER_OPPFOLGING_MED_SIDEEFFEKT = "veilarboppfolging.ikke_oppdater_oppfolging_med_sideeffekt";
    private static final String LAGRE_VEILEDER_SOM_HAR_UTFORT_TILORDNING = "veilarboppfolging.lagre_veileder_som_har_utfort_tilordning";
    private static final String SKAL_IGNORERE_GAMLE_ENDRINGER_FRA_VEILARBARENA = "veilarboppfolging.skal_ignorere_gamle_endringer_fra_veilarbarena";

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

    public boolean skalIgnorereGamleEndringerFraVeilarbarena() {
        return unleashClient.isEnabled(SKAL_IGNORERE_GAMLE_ENDRINGER_FRA_VEILARBARENA);
    }
}
