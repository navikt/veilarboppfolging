package no.nav.veilarboppfolging.kafka;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.utils.IdUtils;
import no.nav.veilarboppfolging.controller.domain.OppfolgingKafkaDTO;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.repository.OppfolgingFeedRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
public class OppfolgingStatusKafkaProducer {

    private final KafkaProducer<String, String> kafkaProducer;
    private final OppfolgingFeedRepository oppfolgingFeedRepository;
    private final AktorregisterClient aktorregisterClient;
    private final String topicName;

    public OppfolgingStatusKafkaProducer(
            KafkaProducer<String, String> kafkaProducer,
            OppfolgingFeedRepository oppfolgingFeedRepository,
            AktorregisterClient aktorregisterClient,
            String topicName
    ) {
        this.kafkaProducer = kafkaProducer;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
        this.aktorregisterClient = aktorregisterClient;
        this.topicName = topicName;
    }

    public void send(Fnr fnr) {
        val fetchAktoerId = supplyAsync(() -> new AktorId(aktorregisterClient.hentAktorId(fnr.getFnr())));
        fetchAktoerId.thenAccept(this::send);
    }

    @SneakyThrows
    public Try<OppfolgingKafkaDTO> send(AktorId aktorId) {
        val aktoerId = aktorId.getAktorId();
        val result = oppfolgingFeedRepository.hentOppfolgingStatus(aktoerId);

        if (result.isFailure()) {
            log.error("Kunne ikke hente oppfølgingsstatus for bruker {} {}", aktoerId, result.getCause());
            return result;
        }
        
        return result
                .onSuccess(this::send)
                .onFailure(t -> log.error("Feilet under sending til kafka for bruker " + aktoerId, t));
    }

    @SneakyThrows
    private RecordMetadata send(OppfolgingKafkaDTO dto) {
        val aktoerId = dto.getAktoerid();
        val header = new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationIdAsBytes());
        val record = new ProducerRecord<>(topicName, 0, aktoerId, toJson(dto), singletonList(header));
        return kafkaProducer.send(record).get();
    }

    public int publiserAlleBrukere() {
        val antallBrukere = oppfolgingFeedRepository.hentAntallBrukere().orElseThrow(IllegalStateException::new);
        val BATCH_SIZE = 1000;
        int offset = 0;

        do {
            List<OppfolgingKafkaDTO> brukere = oppfolgingFeedRepository.hentOppfolgingStatus(offset);
            brukere.forEach(this::send);
            offset = offset + BATCH_SIZE;
        }
        while (offset <= antallBrukere);

        return offset;
    }

    static byte[] getCorrelationIdAsBytes() {
        String correlationId = MDC.get(PREFERRED_NAV_CALL_ID_HEADER_NAME);

        if (correlationId == null) {
            correlationId = MDC.get("jobId");
        }
        if (correlationId == null) {
            correlationId = IdUtils.generateId();
        }

        return correlationId.getBytes();
    }
}
