package no.nav.veilarboppfolging.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import no.nav.veilarboppfolging.kafka.AvsluttOppfolgingProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static no.nav.common.leaderelection.LeaderElection.isLeader;

@Slf4j
@Component
public class AvsluttOppfolgingKafkaFeilSchedule {

    public static final long SCHEDULE_DELAY =  15 * 1000; // 15 sec

    private AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;

    private AvsluttOppfolgingProducer avsluttOppfolgingProducer;

    @Inject
    public AvsluttOppfolgingKafkaFeilSchedule(AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository, AvsluttOppfolgingProducer avsluttOppfolgingProducer) {
        this.avsluttOppfolgingEndringRepository = avsluttOppfolgingEndringRepository;
        this.avsluttOppfolgingProducer = avsluttOppfolgingProducer;
    }

    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay =  5 * 1000)
    public void sendFeiledeKafkaMeldinger() {
        if(isLeader()) {
            List<AvsluttOppfolgingKafkaDTO> avsluttOppfolgingKafkaBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
            log.info("Starter jobb for legge til avslutning av {} brukere på kafka", avsluttOppfolgingKafkaBrukere.size());
            avsluttOppfolgingKafkaBrukere.forEach(feiletMelding -> avsluttOppfolgingProducer.avsluttOppfolgingEvent(feiletMelding.getAktorId(), feiletMelding.getSluttdato()));
        }
    }

}

