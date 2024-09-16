package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.ForbiddenException;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.MaalRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
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
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

@Service
public class MaalService {

    private final KafkaProducerService kafkaProducerService;

    private final MetricsService metricsService;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final KvpRepository kvpRepository;

    private final AuthService authService;

    private final MaalRepository maalRepository;

    private final TransactionTemplate transactor;

    @Autowired
    public MaalService(
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
        authService.sjekkLesetilgangMedFnr(fnr);

        Optional<OppfolgingEntity> maybeOppfolging = oppfolgingsStatusRepository.hentOppfolging(aktorId);

        long gjeldendeMaalId = maybeOppfolging.map(OppfolgingEntity::getGjeldendeMaalId).orElse(0L);

        Optional<MaalEntity> maybeGjeldendeMaal = empty();

        if (gjeldendeMaalId != 0) {
            maybeGjeldendeMaal = maalRepository.hentMaal(gjeldendeMaalId);
        }

        if (maybeGjeldendeMaal.isEmpty()) {
            return new MaalEntity();
        }

        MaalEntity gjeldendeMaal = maybeGjeldendeMaal.get();

        List<KvpPeriodeEntity> kvpList = kvpRepository.hentKvpHistorikk(aktorId);

        if (!KvpUtils.sjekkTilgangGittKvp(authService, kvpList, gjeldendeMaal::getDato)) {
            return new MaalEntity();
        }

        return gjeldendeMaal;
    }

    public List<MaalEntity> hentMaalList(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedFnr(fnr);

        List<MaalEntity> malList = maalRepository.aktorMal(aktorId);

        List<KvpPeriodeEntity> kvpList = kvpRepository.hentKvpHistorikk(aktorId);
        return malList.stream().filter(mal -> KvpUtils.sjekkTilgangGittKvp(authService, kvpList, mal::getDato)).collect(toList());
    }

    public MaalEntity oppdaterMaal(String mal, Fnr fnr, String endretAvVeileder) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkSkrivetilgangMedAktorId(aktorId);

        Optional<KvpPeriodeEntity> maybeKvpPeriode = kvpRepository.hentKvpPeriode(kvpRepository.gjeldendeKvp(aktorId));
        maybeKvpPeriode.ifPresent(this::sjekkKvpEnhetTilgang);

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

    private void sjekkKvpEnhetTilgang(KvpPeriodeEntity kvp) {
        if (!authService.harTilgangTilEnhetMedSperre(kvp.getEnhet())) {
            throw new ForbiddenException("Har ikke skrivetilgang til enhet med sperre");
        }
    }

}
