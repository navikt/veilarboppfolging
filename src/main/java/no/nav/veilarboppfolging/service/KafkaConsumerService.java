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
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
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

        if (skalIgnorereIkkeEksisterendeBrukereIDev(brukerFnr)) {
            log.info("Velger å ikke behandle ugyldig bruker i dev miljøet.");
            return;
        }

        if (erEndringGammel(brukerFnr, endringPaBruker.getSistEndretDato())) {
            log.info("Endring på oppfølgingsbruker fra Arena er eldre enn sist lagret endring. " +
                    "Dersom vi ikke utførte en rewind på topicen betyr dette at Arena har en uventet oppførsel. " +
                    "Denne loggmeldingen er kun til informasjon slik at vi eventuelt kan fange opp dette scenariet til ettertid.");
        }

        var context = new AuthContext(
                UserRole.SYSTEM,
                JWTParser.parse(systemUserTokenProvider.getSystemUserToken())
        );

        authContextHolder.withContext(context, () -> {
            kvpService.avsluttKvpVedEnhetBytte(endringPaBruker);
            iservService.oppdaterUtmeldingsStatus(endringPaBruker);
            oppfolgingsenhetEndringService.behandleBrukerEndring(endringPaBruker);
            oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(endringPaBruker);
            sisteEndringPaaOppfolgingBrukerService.lagreSisteEndring(brukerFnr, endringPaBruker.getSistEndretDato());
        });
    }

    private boolean erEndringGammel(Fnr fnr, ZonedDateTime nyEndringTidspunkt) {

        //everything before this date was ok, no need to re-read old messages
        ZonedDateTime thresholdDato = ZonedDateTime.of(2022, 12, 12, 12, 0, 0, 0, ZoneId.systemDefault());

        if (nyEndringTidspunkt.isBefore(thresholdDato)) {
            return true;
        }

        Optional<ZonedDateTime> sisteRegistrerteEndringTidspunkt = sisteEndringPaaOppfolgingBrukerService.hentSisteEndringDato(fnr);

        return sisteRegistrerteEndringTidspunkt
                .map(sisteRegistrerteEndring -> sisteRegistrerteEndring.isAfter(nyEndringTidspunkt) || sisteRegistrerteEndring.equals(nyEndringTidspunkt))
                .orElse(false);
    }

    private boolean skalIgnorereIkkeEksisterendeBrukereIDev(Fnr fnr) {
        if (isDevelopment().orElse(false)) {
            try {
                aktorOppslagClient.hentAktorId(fnr);
            } catch (IngenGjeldendeIdentException e) {
                return true;
            }
        }
        return false;
    }

}
