package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.MaalRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.KvpEntity;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import no.nav.veilarboppfolging.utils.KvpUtils;
import no.nav.veilarboppfolging.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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

    private final TransactionTemplate transactor;

    @Autowired
    public MalService(
            KafkaProducerService kafkaProducerService,
            MetricsService metricsService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            KvpRepository kvpRepository,
            AuthService authService,
            MaalRepository maalRepository,
            TransactionTemplate transactor
    ) {
        this.kafkaProducerService = kafkaProducerService;
        this.metricsService = metricsService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.kvpRepository = kvpRepository;
        this.authService = authService;
        this.maalRepository = maalRepository;
        this.transactor = transactor;
    }

    public MaalEntity hentMal(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);

        OppfolgingEntity oppfolgingsStatus = oppfolgingsStatusRepository.fetch(aktorId);

        MaalEntity gjeldendeMal = null;

        if (oppfolgingsStatus.getGjeldendeMaalId() != 0) {
            gjeldendeMal = maalRepository.fetch(oppfolgingsStatus.getGjeldendeMaalId());
        }

        if (gjeldendeMal == null) {
            return new MaalEntity();
        }

        List<KvpEntity> kvpList = kvpRepository.hentKvpHistorikk(aktorId);
        if (!KvpUtils.sjekkTilgangGittKvp(authService, kvpList, gjeldendeMal::getDato)) {
            return new MaalEntity();
        }

        return gjeldendeMal;
    }

    public List<MaalEntity> hentMalList(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);

        List<MaalEntity> malList = maalRepository.aktorMal(aktorId);

        List<KvpEntity> kvpList = kvpRepository.hentKvpHistorikk(aktorId);
        return malList.stream().filter(mal -> KvpUtils.sjekkTilgangGittKvp(authService, kvpList, mal::getDato)).collect(toList());
    }

    public MaalEntity oppdaterMal(String mal, Fnr fnr, String endretAvVeileder) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkSkrivetilgangMedAktorId(aktorId);

        KvpEntity kvp = kvpRepository.fetch(kvpRepository.gjeldendeKvp(aktorId));
        ofNullable(kvp).ifPresent(this::sjekkKvpEnhetTilgang);

        MaalEntity malData = new MaalEntity()
                .setAktorId(aktorId.get())
                .setMal(mal)
                .setEndretAv(StringUtils.of(endretAvVeileder).orElse(aktorId.get()))
                .setDato(ZonedDateTime.now());

        transactor.executeWithoutResult((ignored) -> {
            maalRepository.opprett(malData);
            kafkaProducerService.publiserEndretMal(aktorId, endretAvVeileder);
        });

        metricsService.oppdatertMittMal(malData, maalRepository.aktorMal(aktorId).size());

        return malData;
    }

    private void sjekkKvpEnhetTilgang(KvpEntity kvp) {
        if (!authService.harTilgangTilEnhetMedSperre(kvp.getEnhet())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

}
