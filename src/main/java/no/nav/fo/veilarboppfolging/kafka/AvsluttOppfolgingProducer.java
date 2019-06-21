package no.nav.fo.veilarboppfolging.kafka;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.AvsluttOppfolgingEndringRepository;
import no.nav.fo.veilarboppfolging.domain.AvsluttOppfolgingKafkaDTO;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Date;
import static no.nav.json.JsonUtils.toJson;

@Slf4j
public class AvsluttOppfolgingProducer {
    private final String topic;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AvsluttOppfolgingEndringRepository avsluttOppfolgingEndringRepository;

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
                sendResult -> onSuccess(avsluttOppfolgingKafkaDTO),
                throwable -> onError(throwable, avsluttOppfolgingKafkaDTO)
        );
    }

    private void onSuccess(AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO) {
        avsluttOppfolgingEndringRepository.deleteAvsluttOppfolgingBruker(avsluttOppfolgingKafkaDTO.getAktorId(), avsluttOppfolgingKafkaDTO.getSluttdato());
        log.info("Bruker med aktorid {} har lagt på {}-topic", avsluttOppfolgingKafkaDTO, this.topic);
    }

    private void onError(Throwable throwable, AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO) {
        avsluttOppfolgingEndringRepository.insertAvsluttOppfolgingBruker(avsluttOppfolgingKafkaDTO.getAktorId());
        log.error("Kunne ikke publisere melding {} til {}-topic", avsluttOppfolgingKafkaDTO, this.topic, throwable);
    }

    public static AvsluttOppfolgingKafkaDTO toDTO (String aktorId, Date sluttdato) {
     return new AvsluttOppfolgingKafkaDTO()
             .setAktorId(aktorId)
             .setSluttdato(sluttdato);
    }
}
