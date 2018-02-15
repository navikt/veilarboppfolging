package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.BehandleArbeidssoekerV1;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;

@Configuration
public class ArenaBehandleArbeidssokerWSConfig {

    public static final String FEATURE_SKIP_REGISTRER_BRUKER_PROPERTY = "feature.skip.registrer.bruker";
    static Supplier<Boolean> FEATURE_SKIP_REGISTRER_BRUKER = () -> parseBoolean(getProperty(FEATURE_SKIP_REGISTRER_BRUKER_PROPERTY, "true"));

    public static final String url = getOptionalProperty("behandlearbeidssoker.endpoint.url").orElse("");

    public static CXFClient<BehandleArbeidssoekerV1> behandleArbeidssokerPortType() {
        return new CXFClient<>(BehandleArbeidssoekerV1.class)
                .address(url);
    }

    @Bean
    @Conditional(TogglePingable.class)
    Pingable behandleArbeidssokerPing() {
        final BehandleArbeidssoekerV1 behandleArbeidssoekerPing = behandleArbeidssokerPortType()
                .configureStsForSystemUserInFSS()
                .build();

        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
                UUID.randomUUID().toString(),
                "BehandleArbeidssoker_V1 via " + url,
                "Ping av BehandleArbeidssoker_V1. Registrerer arbeidssoker i Arena.",
                false
        );

        return () -> {
            try {
                behandleArbeidssoekerPing.ping();
                return lyktes(metadata);
            } catch (Exception e) {
                return feilet(metadata, e);
            }
        };
    }

    public static class TogglePingable implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !FEATURE_SKIP_REGISTRER_BRUKER.get();
        }
    }

}
