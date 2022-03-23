package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.kafka.producer.serializer.JsonValidationSerializer;
import no.nav.common.types.identer.AktorId;
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

    public void publiserOppfolgingsperiode(SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1) {
        sisteOppfolgingsPeriode(sisteOppfolgingsperiodeV1);
        oppfolingsperiode(sisteOppfolgingsperiodeV1);
    }

    private void sisteOppfolgingsPeriode(SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1) {
        store(kafkaProperties.getSisteOppfolgingsperiodeTopic(),
                sisteOppfolgingsperiodeV1.getAktorId(),
                sisteOppfolgingsperiodeV1);
    }

    private void oppfolingsperiode(SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1) {
        store(kafkaProperties.getOppfolgingsperiodeTopic(),
                sisteOppfolgingsperiodeV1.getAktorId(),
                sisteOppfolgingsperiodeV1);
    }

    public void publiserSisteTilordnetVeileder(SisteTilordnetVeilederV1 recordValue) {
        store(kafkaProperties.getSisteTilordnetVeilederTopic(), recordValue.getAktorId(), recordValue);
    }

    public void publiserEndringPaManuellStatus(AktorId aktorId, boolean erManuell) {
        EndringPaManuellStatusV1 recordValue = new EndringPaManuellStatusV1(aktorId.get(), erManuell);
        store(kafkaProperties.getEndringPaManuellStatusTopic(), recordValue.getAktorId(), recordValue);
    }

    public void publiserEndringPaNyForVeileder(AktorId aktorId, boolean erNyForVeileder) {
        EndringPaNyForVeilederV1 recordValue = new EndringPaNyForVeilederV1(aktorId.get(), erNyForVeileder);
        store(kafkaProperties.getEndringPaNyForVeilederTopic(), aktorId.get(), recordValue);
    }

    public void publiserVeilederTilordnet(AktorId aktorId, String tildeltVeilederId) {
        VeilederTilordnetV1 recordValue = new VeilederTilordnetV1(aktorId.get(), tildeltVeilederId);
        store(kafkaProperties.getVeilederTilordnetTopic(), aktorId.get(), recordValue);
    }

    public void publiserOppfolgingStartet(AktorId aktorId, ZonedDateTime oppfolgingStartet) {
        OppfolgingStartetV1 recordValue = new OppfolgingStartetV1(aktorId.get(), oppfolgingStartet);
        store(kafkaProperties.getOppfolgingStartetTopic(), aktorId.get(), recordValue);
    }

    public void publiserOppfolgingAvsluttet(AktorId aktorId) {
        OppfolgingAvsluttetV1 recordValue = new OppfolgingAvsluttetV1(aktorId.get(), ZonedDateTime.now());

        store(kafkaProperties.getOppfolgingAvsluttetTopic(), aktorId.get(), recordValue);

        // Deprecated
        store(kafkaProperties.getEndringPaaAvsluttOppfolgingTopic(), aktorId.get(), recordValue);
    }

    public void publiserKvpStartet(AktorId aktorId, String enhetId, String opprettetAvVeilederId, String begrunnelse, ZonedDateTime startDato) {
        KvpStartetV1 recordValue = KvpStartetV1.builder()
                .aktorId(aktorId.get())
                .enhetId(enhetId)
                .opprettetAv(opprettetAvVeilederId)
                .opprettetBegrunnelse(begrunnelse)
                .opprettetDato(startDato)
                .build();

        store(kafkaProperties.getKvpStartetTopic(), aktorId.get(), recordValue);
    }

    public void publiserKvpAvsluttet(AktorId aktorId, String avsluttetAv, String begrunnelse, ZonedDateTime sluttDato) {
        KvpAvsluttetV1 recordValue = KvpAvsluttetV1.builder()
                .aktorId(aktorId.get())
                .avsluttetAv(avsluttetAv) // veilederId eller System
                .avsluttetBegrunnelse(begrunnelse)
                .avsluttetDato(sluttDato)
                .build();

        store(kafkaProperties.getKvpAvlsuttetTopic(), aktorId.get(), recordValue);
    }

    public void publiserEndretMal(AktorId aktorId, String veilederIdent) {
        EndringPaMalV1 recordValue = EndringPaMalV1.builder()
                .aktorId(aktorId.get())
                .endretTidspunk(ZonedDateTime.now())
                .veilederIdent(veilederIdent)
                .lagtInnAv(
                        authContextHolder.erEksternBruker()
                            ? EndringPaMalV1.InnsenderData.BRUKER
                            : EndringPaMalV1.InnsenderData.NAV
                )
                .build();

        store(kafkaProperties.getEndringPaMalTopic(), aktorId.get(), recordValue);
    }

    private void store(String topic, String key, Object value) {
        ProducerRecord<byte[], byte[]> record = serializeJsonRecord(new ProducerRecord<>(topic, key, value));
        producerRecordStorage.store(record);
    }

}
