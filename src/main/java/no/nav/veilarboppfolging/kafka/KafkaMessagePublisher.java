package no.nav.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.FeiletKafkaMelding;
import no.nav.veilarboppfolging.domain.kafka.*;
import no.nav.veilarboppfolging.repository.FeiletKafkaMeldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.toJson;

@Slf4j
@Component
public class KafkaMessagePublisher {

    private final KafkaTopics kafkaTopics;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final FeiletKafkaMeldingRepository feiletKafkaMeldingRepository;

    @Autowired
    public KafkaMessagePublisher(KafkaTopics kafkaTopics, KafkaTemplate<String, String> kafkaTemplate, FeiletKafkaMeldingRepository feiletKafkaMeldingRepository) {
        this.kafkaTopics = kafkaTopics;
        this.kafkaTemplate = kafkaTemplate;
        this.feiletKafkaMeldingRepository = feiletKafkaMeldingRepository;
    }

    public void publiserEndringPaManuellStatus(EndringPaManuellStatusKafkaDTO endringPaManuellStatusKafkaDTO) {
        publiser(kafkaTopics.getEndringPaManuellStatus(), endringPaManuellStatusKafkaDTO.getAktorId(), toJson(endringPaManuellStatusKafkaDTO));
    }

    public void publiserEndringPaNyForVeileder(EndringPaNyForVeilederKafkaDTO endringPaNyForVeilederKafkaDTO) {
        publiser(kafkaTopics.getEndringPaNyForVeileder(), endringPaNyForVeilederKafkaDTO.getAktorId(), toJson(endringPaNyForVeilederKafkaDTO));
    }

    public void publiserVeilederTilordnet(VeilederTilordnetKafkaDTO veilederTilordnetKafkaDTO) {
        publiser(kafkaTopics.getVeilederTilordnet(), veilederTilordnetKafkaDTO.getAktorId(), toJson(veilederTilordnetKafkaDTO));
    }

    // Deprecated
    public void publiserEndringPaAvsluttOppfolging(OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO) {
        publiser(kafkaTopics.getEndringPaaAvsluttOppfolging(), oppfolgingAvsluttetKafkaDTO.getAktorId(), toJson(oppfolgingAvsluttetKafkaDTO));
    }

    public void publiserOppfolgingStartet(OppfolgingStartetKafkaDTO oppfolgingStartetKafkaDTO) {
        publiser(kafkaTopics.getOppfolgingStartet(), oppfolgingStartetKafkaDTO.getAktorId(), toJson(oppfolgingStartetKafkaDTO));
    }

    public void publiserOppfolgingAvsluttet(OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO) {
        publiser(kafkaTopics.getOppfolgingAvsluttet(), oppfolgingAvsluttetKafkaDTO.getAktorId(), toJson(oppfolgingAvsluttetKafkaDTO));
    }

    public void publiserKvpStartet(KvpStartetKafkaDTO kvpStartetKafkaDTO) {
        publiser(kafkaTopics.getKvpStartet(), kvpStartetKafkaDTO.getAktorId(), toJson(kvpStartetKafkaDTO));
    }

    public void publiserKvpAvsluttet(KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO) {
        publiser(kafkaTopics.getKvpAvlsuttet(), kvpAvsluttetKafkaDTO.getAktorId(), toJson(kvpAvsluttetKafkaDTO));
    }

    public void publiserTidligereFeiletMelding(FeiletKafkaMelding feiletKafkaMelding) {
        kafkaTemplate.send(feiletKafkaMelding.getTopicName(), feiletKafkaMelding.getMessageKey(), feiletKafkaMelding.getJsonPayload())
                .addCallback(
                        sendResult -> onSuccessTidligereFeilet(feiletKafkaMelding),
                        throwable -> onErrorTidligereFeilet(feiletKafkaMelding, throwable)
                );
    }

    private void publiser(String topicName, String messageKey, String jsonPayload) {
        kafkaTemplate.send(topicName, messageKey, jsonPayload)
                .addCallback(
                        sendResult -> onSuccess(topicName, messageKey),
                        throwable -> onError(topicName, messageKey, jsonPayload, throwable)
                );
    }

    private void onSuccess(String topic, String key) {
        log.info(format("Publiserte melding på topic %s med key %s", topic, key));
    }

    private void onError(String topicName, String messageKey, String jsonPayload, Throwable throwable) {
        log.error(format("Kunne ikke publisere melding på topic %s med key %s \nERROR: %s", topicName, messageKey, throwable));
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding(topicName, messageKey, jsonPayload);
    }

    private void onSuccessTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding) {
        String topicName = feiletKafkaMelding.getTopicName();
        String messageKey = feiletKafkaMelding.getMessageKey();

        log.info(format("Publiserte tidligere feilet melding på topic %s med key %s", topicName, messageKey));
        feiletKafkaMeldingRepository.slettFeiletKafkaMelding(feiletKafkaMelding.getId());
    }

    private void onErrorTidligereFeilet(FeiletKafkaMelding feiletKafkaMelding, Throwable throwable) {
        String topicName = feiletKafkaMelding.getTopicName();
        String messageKey = feiletKafkaMelding.getMessageKey();

        log.error(format("Kunne ikke publisere tidligere feilet melding på topic %s med key %s \nERROR: %s", topicName, messageKey, throwable));
    }

}
