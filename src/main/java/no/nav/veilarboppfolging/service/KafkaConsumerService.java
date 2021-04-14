package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import org.springframework.beans.factory.annotation.Autowired;
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
            KvpService kvpService,
            MetricsService metricsService,
            IservService iservService,
            OppfolgingsenhetEndringService oppfolgingsenhetEndringService
    ) {
        this.authContextHolder = authContextHolder;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.kvpService = kvpService;
        this.metricsService = metricsService;
        this.iservService = iservService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
    }

    @SneakyThrows
    public void consumeEndringPaOppfolgingBruker(VeilarbArenaOppfolgingEndret kafkaMelding) {
        try {

            var context = new AuthContext(
                    UserRole.SYSTEM,
                    JWTParser.parse(systemUserTokenProvider.getSystemUserToken())
            );

            authContextHolder.withContext(context, () -> {
                kvpService.avsluttKvpVedEnhetBytte(kafkaMelding);
                iservService.behandleEndretBruker(kafkaMelding);
                oppfolgingsenhetEndringService.behandleBrukerEndring(kafkaMelding);
            });
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding for endring på oppfølgingsbruker:\n{}", t.getMessage(), t);
            throw t;
        } finally {
            metricsService.antallMeldingerKonsumertAvKafka();
        }
    }

}
