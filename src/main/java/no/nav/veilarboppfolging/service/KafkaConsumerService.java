package no.nav.veilarboppfolging.service;

import com.nimbusds.jwt.JWTParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

@Slf4j
@Service
public class KafkaConsumerService {

    private final AuthContextHolder authContextHolder;

    private final SystemUserTokenProvider systemUserTokenProvider;

    private final KvpService kvpService;

    private final IservService iservService;

    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;

    private final OppfolgingEndringService oppfolgingEndringService;

    private final AktorOppslagClient aktorOppslagClient;

    private final SisteEndringPaaOppfolgingBrukerService sisteEndringPaaOppfolgingBrukerService;

    @Autowired
    public KafkaConsumerService(
            AuthContextHolder authContextHolder,
            SystemUserTokenProvider systemUserTokenProvider,
            @Lazy KvpService kvpService,
            @Lazy IservService iservService,
            OppfolgingsenhetEndringService oppfolgingsenhetEndringService,
            @Lazy OppfolgingEndringService oppfolgingEndringService,
            AktorOppslagClient aktorOppslagClient,
            SisteEndringPaaOppfolgingBrukerService sisteEndringPaaOppfolgingBrukerService) {
        this.authContextHolder = authContextHolder;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.kvpService = kvpService;
        this.iservService = iservService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
        this.oppfolgingEndringService = oppfolgingEndringService;
        this.aktorOppslagClient = aktorOppslagClient;
        this.sisteEndringPaaOppfolgingBrukerService = sisteEndringPaaOppfolgingBrukerService;
    }

    @SneakyThrows
    public void consumeEndringPaOppfolgingBruker(ConsumerRecord<String, EndringPaaOppfoelgingsBrukerV2> kafkaMelding) {
        EndringPaaOppfoelgingsBrukerV2 endringPaBruker = kafkaMelding.value();

        Fnr brukerFnr = Fnr.of(endringPaBruker.getFodselsnummer());

        if(skalIgnorereIkkeEksisterendeBrukereIDev(brukerFnr)){
            log.info("Velger å ikke behnadle ugyldig bruker i dev miljøet.");
            return;
        }

        Optional<ZonedDateTime> sisteEndringPaaOppfolgingBruker = sisteEndringPaaOppfolgingBrukerService.hentSisteEndringDato(brukerFnr);

        if (sisteEndringPaaOppfolgingBruker.isPresent() && sisteEndringPaaOppfolgingBruker.get().isAfter(endringPaBruker.getSistEndretDato())){
            log.info("Velger å ikke behnadle gamle kafka meldinger for bruker");
            return;
        }

        var context = new AuthContext(
                UserRole.SYSTEM,
                JWTParser.parse(systemUserTokenProvider.getSystemUserToken())
        );

        authContextHolder.withContext(context, () -> {
            kvpService.avsluttKvpVedEnhetBytte(endringPaBruker);
            iservService.behandleEndretBruker(endringPaBruker);
            oppfolgingsenhetEndringService.behandleBrukerEndring(endringPaBruker);
            oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaBruker);
            sisteEndringPaaOppfolgingBrukerService.lagreSisteEndring(brukerFnr, endringPaBruker.getSistEndretDato());
        });
    }

    private boolean skalIgnorereIkkeEksisterendeBrukereIDev(Fnr fnr){
        if(isDevelopment().orElse(false)){
            try {
                aktorOppslagClient.hentAktorId(fnr);
            } catch (IngenGjeldendeIdentException e){
                return true;
            }
        }
        return false;
    }

}
