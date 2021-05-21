package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.veilarboppfolging.domain.Tilordning;
import no.nav.veilarboppfolging.domain.kafka.OppfolgingsperiodeKafkaDto;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
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
            List<String> unikeAktorIder = oppfolgingsStatusRepository.hentUnikeBrukerePage(currentOffset, OPPFOLGINGSPERIODE_PAGE_SIZE);

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
            List<String> unikeAktorIder = oppfolgingsStatusRepository.hentUnikeBrukerePage(currentOffset, OPPFOLGINGSPERIODE_PAGE_SIZE);

            if (unikeAktorIder.isEmpty()) {
                break;
            }

            currentOffset += unikeAktorIder.size();

            log.info("Republiserer tilordnet veileder. CurrentOffset={} BatchSize={}", currentOffset, unikeAktorIder.size());

            unikeAktorIder.forEach(this::republiseTilordnetVeilederForBruker);
        }
    }

    private void republiserOppfolgingsperiodeForBruker(String aktorId) {
        List<Oppfolgingsperiode> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        Oppfolgingsperiode sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);
        OppfolgingsperiodeKafkaDto kafkaDto = DtoMappers.tilOppfolgingsperiodeKafkaDto(sistePeriode);

        kafkaProducerService.publiserSisteOppfolgingsperiode(kafkaDto);
    }

    private void republiseTilordnetVeilederForBruker(String aktorId) {
        Optional<Tilordning> maybeTilordning = veilederTilordningerRepository.hentTilordnetVeileder(aktorId);

        maybeTilordning.ifPresent(tilordning -> {
            var dto = DtoMappers.tilSisteVeilederTilordnetKafkaDTO(tilordning);
            kafkaProducerService.publiserSisteVeilederTilordnet(dto);
        });
    }

}
