package no.nav.fo.veilarboppfolging.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.OppfolgingKafkaFeiletMeldingRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static no.nav.common.leaderelection.LeaderElection.isLeader;

@Slf4j
@Component
public class OppfolgingKafkaFeilSchedule {

    private static final long FIFTEEN_SECONDS =  15 * 1000;
    private static final long FIVE_SECONDS = 5 * 1000;

    private final OppfolgingKafkaFeiletMeldingRepository repository;
    private final OppfolgingKafkaProducer producer;

    @Inject
    public OppfolgingKafkaFeilSchedule(OppfolgingKafkaFeiletMeldingRepository repository, OppfolgingKafkaProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Scheduled(fixedDelay = FIFTEEN_SECONDS, initialDelay = FIVE_SECONDS)
    public void sendFeiledeKafkaMeldinger() {
        if(isLeader()) {
            List<AktorId> aktorIds = repository.hentFeiledeMeldinger();
            log.info("Starter jobb for legge til avslutning av {} brukere på kafka", aktorIds.size());
            aktorIds.forEach(producer::sendAsync);
        }
    }

}

