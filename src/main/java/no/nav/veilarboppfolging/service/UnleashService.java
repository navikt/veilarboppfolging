package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnleashService {

    private final static String OPPDATER_OPPFOLGING_KAFKA = "veilarboppfolging.oppdater_oppfolging_kafka";

    private final UnleashClient unleashClient;

    public boolean skalOppdaterOppfolgingMedKafka() {
        return unleashClient.isEnabled(OPPDATER_OPPFOLGING_KAFKA);
    }

}
