package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.kafka.KvpPeriode;
import no.nav.veilarboppfolging.kafka.dto.OppfolgingsperiodeDTO;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaRepubliseringService {

    private final static int OPPFOLGINGSPERIODE_PAGE_SIZE = 1000;
    private final static int KVPPERIODE_PAGE_SIZE = 1000;

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final VeilederTilordningerRepository veilederTilordningerRepository;

    private final KvpRepository kvpRepository;

    private final KafkaProducerService kafkaProducerService;

    public void republiserOppfolgingsperioder() {
        int currentOffset = 0;

        while (true) {
            List<AktorId> unikeAktorIder = oppfolgingsStatusRepository.hentUnikeBrukerePage(currentOffset, OPPFOLGINGSPERIODE_PAGE_SIZE);

            if (unikeAktorIder.isEmpty()) {
                break;
            }

            currentOffset += unikeAktorIder.size();

            log.info("Republiserer oppfolgingsperioder. CurrentOffset={} BatchSize={}", currentOffset, unikeAktorIder.size());

            unikeAktorIder.forEach(this::republiserOppfolgingsperiodeForBruker);
        }
    }

    public void republiserTilordnetVeileder() {
        int currentOffset = 0;

        while (true) {
            List<AktorId> unikeAktorIder = oppfolgingsStatusRepository.hentUnikeBrukerePage(currentOffset, OPPFOLGINGSPERIODE_PAGE_SIZE);

            if (unikeAktorIder.isEmpty()) {
                break;
            }

            currentOffset += unikeAktorIder.size();

            log.info("Republiserer tilordnet veileder. CurrentOffset={} BatchSize={}", currentOffset, unikeAktorIder.size());

            unikeAktorIder.forEach(this::republiserSisteTilordnetVeilederForBruker);
        }
    }

    public void republiserKvpPerioder() {
        int currentOffset = 0;

        while (true) {
            List<KvpPeriodeEntity> kvpPerioder = kvpRepository.hentKvpPerioderPage(currentOffset, KVPPERIODE_PAGE_SIZE);

            if (kvpPerioder.isEmpty()) {
                break;
            }

            currentOffset += kvpPerioder.size();

            log.info("Republiserer kvp perioder. CurrentOffset={} BatchSize={}", currentOffset, kvpPerioder.size());

            kvpPerioder.forEach(this::republiserKvpPeriode);
        }
    }

    private void republiserKvpPeriode(KvpPeriodeEntity kvpPeriodeEntity) {
        KvpPeriode startetKvpPeriode = KvpPeriode.start(
                AktorId.of(kvpPeriodeEntity.getAktorId()),
                kvpPeriodeEntity.getEnhet(),
                kvpPeriodeEntity.getOpprettetAv(),
                kvpPeriodeEntity.getOpprettetDato(),
                kvpPeriodeEntity.getOpprettetBegrunnelse()
        );
        kafkaProducerService.publiserKvpPeriode(startetKvpPeriode);

        if (kvpPeriodeEntity.getAvsluttetDato() != null) {
            KvpPeriode avsluttetKvpPeriode = startetKvpPeriode.avslutt(
                    kvpPeriodeEntity.getAvsluttetAv(),
                    kvpPeriodeEntity.getAvsluttetDato(),
                    kvpPeriodeEntity.getAvsluttetBegrunnelse()
            );
            kafkaProducerService.publiserKvpPeriode(avsluttetKvpPeriode);
        }
    }

    public void republiserOppfolgingsperiodeForBruker(AktorId aktorId) {
        List<OppfolgingsperiodeEntity> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        OppfolgingsperiodeEntity sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);

        if (sistePeriode == null) {
            secureLog.error("Bruker med aktorId={} ligger i OPPFOLGINGSTATUS men har ingen oppfølgingsperioder", aktorId);
            return;
        }

        OppfolgingsperiodeDTO periodeDTO = DtoMappers.tilOppfolgingsperiodeDTO(sistePeriode);

        kafkaProducerService.publiserOppfolgingsperiode(periodeDTO);
    }

    private void republiserSisteTilordnetVeilederForBruker(AktorId aktorId) {
        Optional<VeilederTilordningEntity> maybeTilordning = veilederTilordningerRepository.hentTilordnetVeileder(aktorId);

        maybeTilordning.ifPresent(tilordning -> {
            // Skal ikke publisere for brukere som ikke har fått veileder
            if (tilordning.getVeilederId() == null) {
                return;
            }

            var dto = DtoMappers.tilSisteTilordnetVeilederKafkaDTO(tilordning);
            kafkaProducerService.publiserSisteTilordnetVeileder(dto);
        });
    }

}
