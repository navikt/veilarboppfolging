package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.veilarboppfolging.domain.kafka.OppfolgingsperiodeKafkaDto;
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository;
import no.nav.veilarboppfolging.utils.DtoMappers;
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaRepubliseringService {

    private final static int OPPFOLGINGSPERIODE_PAGE_SIZE = 1000;

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private final KafkaProducerService kafkaProducerService;

    public void republiserOppfolgingsperioder() {
        int currentOffset = 0;

        while (true) {
            List<String> unikeAktorIder = oppfolgingsPeriodeRepository.hentUnikeBrukerePage(currentOffset, OPPFOLGINGSPERIODE_PAGE_SIZE);

            if (unikeAktorIder.isEmpty()) {
                break;
            }

            currentOffset += unikeAktorIder.size();

            log.info("Republiserer oppfolgingsperioder. CurrentOffset={} BatchSize={}", currentOffset, unikeAktorIder.size());

            unikeAktorIder.forEach(this::republiserOppfolgingsperiodeForBruker);
        }
    }

    private void republiserOppfolgingsperiodeForBruker(String aktorId) {
        List<Oppfolgingsperiode> perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId);
        Oppfolgingsperiode sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder);
        OppfolgingsperiodeKafkaDto kafkaDto = DtoMappers.tilOppfolgingsperiodeKafkaDto(sistePeriode);

        kafkaProducerService.publiserSisteOppfolgingsperiode(kafkaDto);
    }

}
