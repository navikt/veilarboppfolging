package no.nav.fo.veilarboppfolging.config;

import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.OpprettBrukerIArenaFeature;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import java.util.UUID;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

@Configuration
public class ArenaBehandleArbeidssokerWSConfig {

    @Inject
    private OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;

    public static final String url = getOptionalProperty("behandlearbeidssoker.endpoint.url").orElse("");

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

        if (!opprettBrukerIArenaFeature.erAktiv()) {
            return () -> lyktes(metadata);
        }

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

}
