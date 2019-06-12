package no.nav.fo.veilarboppfolging.kafka;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.fo.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;

import static no.nav.json.JsonUtils.toJson;
@Component
@Slf4j
public class AvsluttOppfolgingProducer {
    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;

    @Inject
    public AvsluttOppfolgingProducer(KafkaTemplate<String, String> kafkaTemplate, AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.avsluttOppfolgingEndringRepository = avsluttOppfolgingEndringRepository;
        this.topic = topic;
    }

    public void avsluttOppfolgingEvent(String aktorId, Date sluttdato) {
        final AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO = toDTO(aktorId, sluttdato);
        final String serialisertBruker = toJson(avsluttOppfolgingKafkaDTO);
        kafkaTemplate.send(
                topic,
                aktorId,
                serialisertBruker
        ).addCallback(
                sendResult -> onSuccess(aktorId),
                throwable -> onError(throwable, avsluttOppfolgingKafkaDTO)
        );
    }

    private void onSuccess(String aktorId) {
        avsluttOppfolgingEndringRepository.deleteFeiletBruker(aktorId);
        log.info("Bruker med aktorid {} har lagt på avsluttoppfolging-topic", aktorId);
    }

    private void onError(Throwable throwable, AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO) {
        log.error("Kunne ikke publisere melding til avsluttoppfolging-topic", throwable);
        log.info("Forsøker å insertere feilede bruker med aktorid {} i FEILEDE_KAFKA_AVSLUTT_OPPFOLGING_BRUKERE", avsluttOppfolgingKafkaDTO.getAktorId());
        avsluttOppfolgingEndringRepository.insertFeiletBruker(avsluttOppfolgingKafkaDTO);
    }

    public static AvsluttOppfolgingKafkaDTO toDTO (String aktorId, Date sluttdato) {
     return new AvsluttOppfolgingKafkaDTO()
             .setAktorId(aktorId)
             .setSluttdato(sluttdato);
    }
}
