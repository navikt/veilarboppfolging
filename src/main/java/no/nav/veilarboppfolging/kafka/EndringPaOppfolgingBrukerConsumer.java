package no.nav.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.service.IservService;
import no.nav.veilarboppfolging.service.MetricsService;
import no.nav.veilarboppfolging.service.OppfolgingsenhetEndringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Component
public class EndringPaOppfolgingBrukerConsumer {

    private final String endringPaaOppfolgingBrukerTopic;

    private final MetricsService metricsService;

    private final IservService iservService;

    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;

    @Autowired
    public EndringPaOppfolgingBrukerConsumer(
            KafkaTopics kafkaTopics,
            MetricsService metricsService,
            IservService iservService,
            OppfolgingsenhetEndringService oppfolgingsenhetEndringService
    ) {
        this.endringPaaOppfolgingBrukerTopic = kafkaTopics.getEndringPaaOppfolgingBruker();
        this.metricsService = metricsService;
        this.iservService = iservService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
    }

    // TODO: Sjekk at dette fungerer
    @KafkaListener(topics = "#{endringPaaOppfolgingBrukerTopic}")
    public void consumeEndringPaOppfolgingBruker(@Payload String kafkaMelding) {
        try {
            final VeilarbArenaOppfolgingEndret deserialisertBruker = deserialisereBruker(kafkaMelding);
            iservService.behandleEndretBruker(deserialisertBruker);
            oppfolgingsenhetEndringService.behandleBrukerEndring(deserialisertBruker);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding: {}\n{}\n{}", endringPaaOppfolgingBrukerTopic, kafkaMelding, t.getMessage(), t);
        } finally {
            metricsService.antallMeldingerKonsumertAvKafka();
        }
    }

    public static VeilarbArenaOppfolgingEndret deserialisereBruker(String arenaBruker) {
        return fromJson(arenaBruker, VeilarbArenaOppfolgingEndret.class);
    }

}
