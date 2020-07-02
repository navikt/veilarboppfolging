package no.nav.veilarboppfolging.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import no.nav.veilarboppfolging.kafka.AvsluttOppfolgingProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AvsluttOppfolgingKafkaFeilSchedule {

    public static final long SCHEDULE_DELAY =  15 * 1000; // 15 sec

    private final AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;

    private final AvsluttOppfolgingProducer avsluttOppfolgingProducer;

    private final LeaderElectionClient leaderElectionClient;

    @Autowired
    public AvsluttOppfolgingKafkaFeilSchedule(
            AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository,
            AvsluttOppfolgingProducer avsluttOppfolgingProducer,
            LeaderElectionClient leaderElectionClient
    ) {
        this.avsluttOppfolgingEndringRepository = avsluttOppfolgingEndringRepository;
        this.avsluttOppfolgingProducer = avsluttOppfolgingProducer;
        this.leaderElectionClient = leaderElectionClient;
    }


    @Scheduled(fixedDelay = SCHEDULE_DELAY, initialDelay =  5 * 1000)
    public void sendFeiledeKafkaMeldinger() {
        if (leaderElectionClient.isLeader()) {
            List<AvsluttOppfolgingKafkaDTO> avsluttOppfolgingKafkaBrukere = avsluttOppfolgingEndringRepository.hentAvsluttOppfolgingBrukere();
            log.info("Starter jobb for legge til avslutning av {} brukere på kafka", avsluttOppfolgingKafkaBrukere.size());
            avsluttOppfolgingKafkaBrukere.forEach(feiletMelding -> avsluttOppfolgingProducer.avsluttOppfolgingEvent(feiletMelding.getAktorId(), feiletMelding.getSluttdato()));
        }
    }

}

