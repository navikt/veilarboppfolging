package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService;
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker;
import no.nav.veilarboppfolging.utils.SecureLog;
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

    private final AuthService authService;
    private final KvpService kvpService;
    private final UtmeldingsService utmeldingsService;
    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;
    private final OppfolgingsbrukerEndretIArenaService oppfolgingsbrukerEndretIArenaService;
    private final AktorOppslagClient aktorOppslagClient;
    private final SisteEndringPaaOppfolgingBrukerService sisteEndringPaaOppfolgingBrukerService;

    @Autowired
    public KafkaConsumerService(
            AuthService authService,
            @Lazy KvpService kvpService,
            @Lazy UtmeldingsService utmeldingsService,
            OppfolgingsenhetEndringService oppfolgingsenhetEndringService,
            @Lazy OppfolgingsbrukerEndretIArenaService oppfolgingsbrukerEndretIArenaService,
            AktorOppslagClient aktorOppslagClient,
            SisteEndringPaaOppfolgingBrukerService sisteEndringPaaOppfolgingBrukerService) {
        this.authService = authService;
        this.kvpService = kvpService;
        this.utmeldingsService = utmeldingsService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
        this.oppfolgingsbrukerEndretIArenaService = oppfolgingsbrukerEndretIArenaService;
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

        try {
            var aktorId = authService.getAktorIdOrThrow(brukerFnr);
            var endring = EndringPaaOppfolgingsBruker.Companion.from(endringPaBruker, aktorId);
            kvpService.avsluttKvpVedEnhetBytte(endring);
            utmeldingsService.oppdaterUtmeldingsStatus(KanskjeIservBruker.Companion.of(endringPaBruker, aktorId));
            oppfolgingsenhetEndringService.behandleBrukerEndring(endring);
            oppfolgingsbrukerEndretIArenaService.oppdaterOppfolgingMedStatusFraArena(endring);
            sisteEndringPaaOppfolgingBrukerService.lagreSisteEndring(brukerFnr, endringPaBruker.getSistEndretDato());
        } catch (IngenGjeldendeIdentException e) {
            log.warn("Fant ikke gjeldende ident ved behandling av endringPaOppfolgingBruker melding");
            SecureLog.secureLog.warn("Fant ikke gjeldende ident for fnr: {} ved behandling av endringPaOppfolgingBruker melding", brukerFnr.get());
        }
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
