package no.nav.veilarboppfolging.kafka;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.veilarboppfolging.domain.FeiletKafkaMelding;
import no.nav.veilarboppfolging.repository.FeiletKafkaMeldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaProducerHealthCheck implements HealthCheck {

    private final FeiletKafkaMeldingRepository feiletKafkaMeldingRepository;

    @Autowired
    public KafkaProducerHealthCheck(FeiletKafkaMeldingRepository feiletKafkaMeldingRepository) {
        this.feiletKafkaMeldingRepository = feiletKafkaMeldingRepository;
    }

    @Override
    public HealthCheckResult checkHealth() {
        List<FeiletKafkaMelding> feiletKafkaMeldinger = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger(1);
        if (feiletKafkaMeldinger.size() > 0) {
            return HealthCheckResult.unhealthy("Sending av Kafka-melding har feilet");
        }
        return HealthCheckResult.healthy();
    }
}
