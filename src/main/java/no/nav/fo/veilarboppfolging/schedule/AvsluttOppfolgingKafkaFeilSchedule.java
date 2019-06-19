package no.nav.fo.veilarboppfolging.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.fo.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import no.nav.fo.veilarboppfolging.kafka.AvsluttOppfolgingProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Component
public class AvsluttOppfolgingKafkaFeilSchedule {

    public static final long SCHEDULE_DELAY = 60 * 60 * 1000; // 60 minutes

    private AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;

    private AvsluttOppfolgingProducer avsluttOppfolgingProducer;

    @Inject
    public AvsluttOppfolgingKafkaFeilSchedule(AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository, AvsluttOppfolgingProducer avsluttOppfolgingProducer) {
        this.avsluttOppfolgingEndringRepository = avsluttOppfolgingEndringRepository;
        this.avsluttOppfolgingProducer = avsluttOppfolgingProducer;
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay = 15 * 60 * 1000)
    public void sendFeiledeKafkaMeldinger() {
        List<AvsluttOppfolgingKafkaDTO> avsluttOppfolgingKafkaBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
        log.info("Starter jobb for legge til avslutning av brukere {} på kafka", avsluttOppfolgingKafkaBrukere);
        avsluttOppfolgingKafkaBrukere.forEach(feiletMelding -> avsluttOppfolgingProducer.avsluttOppfolgingEvent(feiletMelding.getAktorId(), feiletMelding.getSluttdato()));
    }

}

