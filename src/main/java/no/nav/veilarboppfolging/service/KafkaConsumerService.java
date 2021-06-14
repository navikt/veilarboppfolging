package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    private final AuthContextHolder authContextHolder;

    private final SystemUserTokenProvider systemUserTokenProvider;

    private final KvpService kvpService;

    private final MetricsService metricsService;

    private final IservService iservService;

    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;

    @Autowired
    public KafkaConsumerService(
            AuthContextHolder authContextHolder,
            SystemUserTokenProvider systemUserTokenProvider,
            MetricsService metricsService,
            @Lazy KvpService kvpService,
            @Lazy IservService iservService,
            OppfolgingsenhetEndringService oppfolgingsenhetEndringService
    ) {
        this.authContextHolder = authContextHolder;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.metricsService = metricsService;
        this.kvpService = kvpService;
        this.iservService = iservService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
    }

    @SneakyThrows
    public void consumeEndringPaOppfolgingBruker(ConsumerRecord<String, EndringPaaOppfoelgingsBrukerV1> kafkaMelding) {
        try {
            EndringPaaOppfoelgingsBrukerV1 oppfolgingEndret = kafkaMelding.value();

            var context = new AuthContext(
                    UserRole.SYSTEM,
                    JWTParser.parse(systemUserTokenProvider.getSystemUserToken())
            );

            authContextHolder.withContext(context, () -> {
                kvpService.avsluttKvpVedEnhetBytte(oppfolgingEndret);
                iservService.behandleEndretBruker(oppfolgingEndret);
                oppfolgingsenhetEndringService.behandleBrukerEndring(oppfolgingEndret);
            });
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding for endring på oppfølgingsbruker:\n{}", t.getMessage(), t);
            throw t;
        } finally {
            metricsService.antallMeldingerKonsumertAvKafka();
        }
    }

}
