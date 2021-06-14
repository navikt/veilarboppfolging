package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.serializer.JsonValidationSerializer;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import no.nav.pto_schema.kafka.json.topic.SisteTilordnetVeilederV1;
import no.nav.pto_schema.kafka.json.topic.onprem.*;
import no.nav.veilarboppfolging.config.KafkaProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

import static no.nav.common.kafka.producer.util.ProducerUtils.*;
import static no.nav.pto_schema.kafka.json.JsonSchemaLocator.getKafkaSchema;

@Service
public class KafkaProducerService {

    private final AuthContextHolder authContextHolder;

    private final KafkaProducerRecordStorage producerRecordStorage;

    private final KafkaProperties kafkaProperties;

    private final Serializer<SisteOppfolgingsperiodeV1> sisteOppfolgingsperiodeV1Serializer
            = new JsonValidationSerializer<>(getKafkaSchema(SisteOppfolgingsperiodeV1.class));

    @Autowired
    public KafkaProducerService(
            AuthContextHolder authContextHolder,
            KafkaProducerRecordStorage producerRecordStorage,
            KafkaProperties kafkaProperties
    ) {
        this.authContextHolder = authContextHolder;
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
    }

    public void publiserSisteOppfolgingsperiode(SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1) {
        var record = new ProducerRecord<>(
                kafkaProperties.getSisteOppfolgingsperiodeTopic(),
                sisteOppfolgingsperiodeV1.getAktorId(),
                sisteOppfolgingsperiodeV1
        );

        producerRecordStorage.store(serializeRecord(record, STRING_SERIALIZER, sisteOppfolgingsperiodeV1Serializer));
    }

    public void publiserSisteTilordnetVeileder(SisteTilordnetVeilederV1 recordValue) {
        store(kafkaProperties.getSisteTilordnetVeilederTopic(), recordValue.getAktorId(), recordValue);
    }

    public void publiserEndringPaManuellStatus(String aktorId, boolean erManuell) {
        EndringPaManuellStatusV1 recordValue = new EndringPaManuellStatusV1(aktorId, erManuell);
        store(kafkaProperties.getEndringPaManuellStatusTopic(), recordValue.getAktorId(), recordValue);
    }

    public void publiserEndringPaNyForVeileder(String aktorId, boolean erNyForVeileder) {
        EndringPaNyForVeilederV1 recordValue = new EndringPaNyForVeilederV1(aktorId, erNyForVeileder);
        store(kafkaProperties.getEndringPaNyForVeilederTopic(), aktorId, recordValue);
    }

    public void publiserVeilederTilordnet(String aktorId, String tildeltVeilederId) {
        VeilederTilordnetV1 recordValue = new VeilederTilordnetV1(aktorId, tildeltVeilederId);
        store(kafkaProperties.getVeilederTilordnetTopic(), aktorId, recordValue);
    }

    public void publiserOppfolgingStartet(String aktorId, ZonedDateTime oppfolgingStartet) {
        OppfolgingStartetV1 recordValue = new OppfolgingStartetV1(aktorId, oppfolgingStartet);
        store(kafkaProperties.getOppfolgingStartetTopic(), aktorId, recordValue);
    }

    public void publiserOppfolgingAvsluttet(String aktorId) {
        OppfolgingAvsluttetV1 recordValue = new OppfolgingAvsluttetV1(aktorId, ZonedDateTime.now());

        store(kafkaProperties.getOppfolgingAvsluttetTopic(), aktorId, recordValue);

        // Deprecated
        store(kafkaProperties.getEndringPaaAvsluttOppfolgingTopic(), aktorId, recordValue);
    }

    public void publiserKvpStartet(String aktorId, String enhetId, String opprettetAvVeilederId, String begrunnelse) {
        KvpStartetV1 recordValue = new KvpStartetV1()
                .setAktorId(aktorId)
                .setEnhetId(enhetId)
                .setOpprettetAv(opprettetAvVeilederId)
                .setOpprettetBegrunnelse(begrunnelse)
                .setOpprettetDato(ZonedDateTime.now());

        store(kafkaProperties.getKvpStartetTopic(), aktorId, recordValue);
    }

    public void publiserKvpAvsluttet(String aktorId, String avsluttetAv, String begrunnelse) {
        KvpAvsluttetV1 recordValue = new KvpAvsluttetV1()
                .setAktorId(aktorId)
                .setAvsluttetAv(avsluttetAv) // veilederId eller System
                .setAvsluttetBegrunnelse(begrunnelse)
                .setAvsluttetDato(ZonedDateTime.now());

        store(kafkaProperties.getKvpAvlsuttetTopic(), aktorId, recordValue);
    }

    public void publiserEndretMal(String aktorId, String veilederIdent){
        EndringPaMalV1 recordValue = new EndringPaMalV1()
                .setAktorId(aktorId)
                .setEndretTidspunk(ZonedDateTime.now())
                .setVeilederIdent(veilederIdent)
                .setLagtInnAv(authContextHolder.erEksternBruker()
                        ? EndringPaMalV1.InnsenderData.BRUKER
                        : EndringPaMalV1.InnsenderData.NAV);

        store(kafkaProperties.getEndringPaMalTopic(), aktorId, recordValue);
    }

    private void store(String topic, String key, Object value) {
        ProducerRecord<byte[], byte[]> record = serializeJsonRecord(new ProducerRecord<>(topic, key, value));
        producerRecordStorage.store(record);
    }

}
