package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
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

    private final IservService iservService;

    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;

    private final OppfolgingEndringService oppfolgingEndringService;

    @Autowired
    public KafkaConsumerService(
            AuthContextHolder authContextHolder,
            SystemUserTokenProvider systemUserTokenProvider,
            @Lazy KvpService kvpService,
            @Lazy IservService iservService,
            OppfolgingsenhetEndringService oppfolgingsenhetEndringService,
            @Lazy OppfolgingEndringService oppfolgingEndringService
    ) {
        this.authContextHolder = authContextHolder;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.kvpService = kvpService;
        this.iservService = iservService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
        this.oppfolgingEndringService = oppfolgingEndringService;
    }

    @SneakyThrows
    public void consumeEndringPaOppfolgingBruker(ConsumerRecord<String, EndringPaaOppfoelgingsBrukerV2> kafkaMelding) {
        EndringPaaOppfoelgingsBrukerV2 endringPaBruker = kafkaMelding.value();

        var context = new AuthContext(
                UserRole.SYSTEM,
                JWTParser.parse(systemUserTokenProvider.getSystemUserToken())
        );

        authContextHolder.withContext(context, () -> {
            kvpService.avsluttKvpVedEnhetBytte(endringPaBruker);
            iservService.behandleEndretBruker(endringPaBruker);
            oppfolgingsenhetEndringService.behandleBrukerEndring(endringPaBruker);
            oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaBruker);
        });
    }

}
