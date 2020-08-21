package no.nav.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.service.IservService;
import no.nav.veilarboppfolging.service.KvpService;
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

    private final KvpService kvpService;

    private final MetricsService metricsService;

    private final IservService iservService;

    private final KafkaTopics kafkaTopics;

    private final OppfolgingsenhetEndringService oppfolgingsenhetEndringService;

    @Autowired
    public EndringPaOppfolgingBrukerConsumer(
            KvpService kvpService,
            KafkaTopics kafkaTopics,
            MetricsService metricsService,
            IservService iservService,
            OppfolgingsenhetEndringService oppfolgingsenhetEndringService
    ) {
        this.kvpService = kvpService;
        this.kafkaTopics = kafkaTopics;
        this.metricsService = metricsService;
        this.iservService = iservService;
        this.oppfolgingsenhetEndringService = oppfolgingsenhetEndringService;
    }

    // 'kafkaTopics' blir hentet inn som en bean, ikke fra klasse instansen
    @KafkaListener(topics = "#{kafkaTopics.getEndringPaaOppfolgingBruker()}")
    public void consumeEndringPaOppfolgingBruker(@Payload String kafkaMelding) {
        try {
            final VeilarbArenaOppfolgingEndret deserialisertBruker = fromJson(kafkaMelding, VeilarbArenaOppfolgingEndret.class);
            kvpService.avsluttKvpVedEnhetBytte(deserialisertBruker);
            iservService.behandleEndretBruker(deserialisertBruker);
            oppfolgingsenhetEndringService.behandleBrukerEndring(deserialisertBruker);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding: {}\n{}\n{}", kafkaTopics.getEndringPaaOppfolgingBruker(), kafkaMelding, t.getMessage(), t);
        } finally {
            metricsService.antallMeldingerKonsumertAvKafka();
        }
    }

}
