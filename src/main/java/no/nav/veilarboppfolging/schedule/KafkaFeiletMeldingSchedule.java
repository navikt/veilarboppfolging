package no.nav.veilarboppfolging.schedule;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.veilarboppfolging.domain.FeiletKafkaMelding;
import no.nav.veilarboppfolging.kafka.KafkaMessagePublisher;
import no.nav.veilarboppfolging.kafka.KafkaTopics;
import no.nav.veilarboppfolging.repository.FeiletKafkaMeldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class KafkaFeiletMeldingSchedule {

    private final static long FIFTEEN_MINUTES = 15 * 60 * 1000;

    private final static long THREE_MINUTES = 3 * 60 * 1000;

    private final KafkaTopics kafkaTopics;

    private final LeaderElectionClient leaderElectionClient;

    private final FeiletKafkaMeldingRepository feiletKafkaMeldingRepository;

    private final KafkaMessagePublisher kafkaMessagePublisher;

    @Autowired
    public KafkaFeiletMeldingSchedule(
            KafkaTopics kafkaTopics,
            LeaderElectionClient leaderElectionClient,
            FeiletKafkaMeldingRepository feiletKafkaMeldingRepository,
            KafkaMessagePublisher kafkaMessagePublisher
    ) {
        this.kafkaTopics = kafkaTopics;
        this.leaderElectionClient = leaderElectionClient;
        this.feiletKafkaMeldingRepository = feiletKafkaMeldingRepository;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    @Scheduled(fixedDelay = FIFTEEN_MINUTES, initialDelay = THREE_MINUTES)
    public void publiserTidligereFeiletOppfolgingStartet() {
        if (leaderElectionClient.isLeader()) {
            List<FeiletKafkaMelding> feiledeMeldinger = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger(kafkaTopics.getOppfolgingStartet());
            feiledeMeldinger.forEach(kafkaMessagePublisher::publiserTidligereFeiletMelding);
        }
    }

}
