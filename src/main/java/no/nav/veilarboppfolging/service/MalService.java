package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.MaalRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.utils.KvpUtils;
import no.nav.veilarboppfolging.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Service
public class MalService {

    private final KafkaProducerService kafkaProducerService;

    private final MetricsService metricsService;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final KvpRepository kvpRepository;

    private final AuthService authService;

    private final MaalRepository maalRepository;

    @Autowired
    public MalService(
            KafkaProducerService kafkaProducerService, MetricsService metricsService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            KvpRepository kvpRepository,
            AuthService authService,
            MaalRepository maalRepository) {
        this.kafkaProducerService = kafkaProducerService;
        this.metricsService = metricsService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.kvpRepository = kvpRepository;
        this.authService = authService;
        this.maalRepository = maalRepository;
    }

    public MalData hentMal(String fnr) {
        String aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);

        OppfolgingTable oppfolgingsStatus = oppfolgingsStatusRepository.fetch(aktorId);

        MalData gjeldendeMal = null;

        if (oppfolgingsStatus.getGjeldendeMaalId() != 0) {
            gjeldendeMal = maalRepository.fetch(oppfolgingsStatus.getGjeldendeMaalId());
        }

        if (gjeldendeMal == null) {
            return new MalData();
        }

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(aktorId);
        if (!KvpUtils.sjekkTilgangGittKvp(authService, kvpList, gjeldendeMal::getDato)) {
            return new MalData();
        }

        return gjeldendeMal;
    }

    public List<MalData> hentMalList(String fnr) {
        String aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);

        List<MalData> malList = maalRepository.aktorMal(aktorId);

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(aktorId);
        return malList.stream().filter(mal -> KvpUtils.sjekkTilgangGittKvp(authService, kvpList, mal::getDato)).collect(toList());
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAvVeileder) {
        String aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkSkrivetilgangMedAktorId(aktorId);

        Kvp kvp = kvpRepository.fetch(kvpRepository.gjeldendeKvp(aktorId));
        ofNullable(kvp).ifPresent(this::sjekkKvpEnhetTilgang);

        MalData malData = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(StringUtils.of(endretAvVeileder).orElse(aktorId))
                .setDato(ZonedDateTime.now());

        maalRepository.opprett(malData);
        metricsService.oppdatertMittMal(malData, maalRepository.aktorMal(aktorId).size());
        kafkaProducerService.publiserEndretMal(aktorId, endretAvVeileder);

        return malData;
    }

    private void sjekkKvpEnhetTilgang(Kvp kvp) {
        if (!authService.harTilgangTilEnhetMedSperre(kvp.getEnhet())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

}
