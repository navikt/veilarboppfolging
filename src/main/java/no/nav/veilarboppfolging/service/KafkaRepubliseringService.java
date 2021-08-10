package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaRepubliseringService {

    private final static int OPPFOLGINGSPERIODE_PAGE_SIZE = 1000;

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final VeilederTilordningerRepository veilederTilordningerRepository;

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

    private void republiserOppfolgingsperiodeForBruker(AktorId aktorId) {
        List<OppfolgingsperiodeEntity> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        OppfolgingsperiodeEntity sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);

        if (sistePeriode == null) {
            log.error("Bruker med aktorId={} ligger i OPPFOLGINGSTATUS men har ingen oppfølgingsperioder", aktorId);
            return;
        }

        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1 = DtoMappers.tilSisteOppfolgingsperiodeV1(sistePeriode);

        kafkaProducerService.publiserSisteOppfolgingsperiode(sisteOppfolgingsperiodeV1);
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

    public void republiserEndringPaNyForVeilederForBrukereUnderOppfolging() {
        List<AktorId> unikeAktorIder = oppfolgingsStatusRepository.hentUnikeBrukereUnderOppfolgingPage();

        log.info("Republiserer endring på ny for veileder for brukere under oppfølging. Antall={}", unikeAktorIder.size());

        unikeAktorIder.forEach(this::republiserEndringPaNyForVeilederForBruker);
    }

    private void republiserEndringPaNyForVeilederForBruker(AktorId aktorId) {
        veilederTilordningerRepository.hentTilordnetVeileder(aktorId)
                .filter(veilederTilordning -> veilederTilordning.getVeilederId() != null)
                .ifPresent(veilederTilordning ->
                kafkaProducerService.publiserEndringPaNyForVeileder(aktorId, veilederTilordning.isNyForVeileder())
        );
    }

}
