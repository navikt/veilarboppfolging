package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnleashService {

    @SuppressWarnings("unused")
    private final UnleashClient unleashClient;

    // If using unleash feature toggles add them here, dont remove this class

}
