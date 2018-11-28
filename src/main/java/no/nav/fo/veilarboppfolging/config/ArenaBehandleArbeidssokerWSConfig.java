package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_PROPERTY;
import static no.nav.sbl.dialogarena.common.cxf.TimeoutFeature.DEFAULT_CONNECTION_TIMEOUT;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class ArenaBehandleArbeidssokerWSConfig {
    private static final int BEHANDLE_ARBEIDSSOKER_RECEIVE_TIMEOUT = 300000;

    public static final String url = getRequiredProperty(VIRKSOMHET_BEHANDLEARBEIDSSOEKER_V1_PROPERTY);

    public static CXFClient<BehandleArbeidssoekerV1> behandleArbeidssokerPortType() {
        return new CXFClient<>(BehandleArbeidssoekerV1.class)
                .address(url);
    }

    @Bean
    Pingable behandleArbeidssokerPing() {
        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
                UUID.randomUUID().toString(),
                "BehandleArbeidssoker_V1 via " + url,
                "Ping av BehandleArbeidssoker_V1. Registrerer arbeidssoker i Arena.",
                true
        );

        final BehandleArbeidssoekerV1 behandleArbeidssoekerPing = behandleArbeidssokerPortType()
                .configureStsForSystemUserInFSS()
                .build();

        return () -> {
            try {
                behandleArbeidssoekerPing.ping();
                return lyktes(metadata);
            } catch (Exception e) {
                return feilet(metadata, e);
            }
        };
    }

    @Bean
    public BehandleArbeidssoekerV1 behandleArbeidssoekerV1() {
        return behandleArbeidssokerPortType()
                .configureStsForSystemUserInFSS()
                .timeout(DEFAULT_CONNECTION_TIMEOUT, BEHANDLE_ARBEIDSSOKER_RECEIVE_TIMEOUT)
                .build();
    }


}
