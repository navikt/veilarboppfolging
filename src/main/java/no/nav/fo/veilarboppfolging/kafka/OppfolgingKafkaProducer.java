package no.nav.fo.veilarboppfolging.kafka;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.utils.IdUtils;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingKafkaDTO;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederTilordning;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.Future;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static no.nav.fo.veilarboppfolging.utils.FnrUtils.getAktorIdOrElseThrow;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
public class OppfolgingKafkaProducer {

    private final KafkaProducer<String, String> kafkaProducer;
    private final OppfolgingFeedRepository repository;
    private final AktorService aktorService;
    private final String topicName;

    public OppfolgingKafkaProducer(KafkaProducer<String, String> kafkaProducer,
                                   OppfolgingFeedRepository repository,
                                   AktorService aktorService, String topicName) {
        this.kafkaProducer = kafkaProducer;
        this.repository = repository;
        this.aktorService = aktorService;
        this.topicName = topicName;
    }

    public void sendAsync(List<VeilederTilordning> tilordninger) {
        tilordninger.stream()
                .map(VeilederTilordning::toAktorId)
                .forEach(this::sendAsync);
    }

    public void sendAsync(Fnr fnr) {
        val fetchAktoerId = supplyAsync(() -> getAktorIdOrElseThrow(aktorService, fnr.getFnr()));
        fetchAktoerId.thenAccept(this::sendAsync);
    }

    public void sendAsync(AktorId aktoerId) {
        runAsync(() -> send(aktoerId));
    }

    @SneakyThrows
    public Try<OppfolgingKafkaDTO> send(AktorId aktorId) {
        val aktoerId = aktorId.getAktorId();
        val result = repository.hentOppfolgingStatus(aktoerId);

        if (result.isFailure()) {
            log.error("Kunne ikke hente oppfølgingsstatus for bruker {} {}", aktoerId, result.getCause());
            return result;
        }
        return result.onSuccess(this::send);
    }

    private Future<RecordMetadata> send(OppfolgingKafkaDTO dto) {
        val aktoerId = dto.getAktoerid();
        val header = new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationIdAsBytes());
        val record = new ProducerRecord<>(topicName, 0, aktoerId, toJson(dto), singletonList(header));

        Callback callback = (metadata, exception) -> {
            if (exception != null) {
                log.error("Kunne ikke publisere oppfølging for bruker {} \n{}", aktoerId, exception.getStackTrace());
            } else {
                log.info("Publiserte oppfølging for bruker {} på topic {}", aktoerId, topicName);
            }
        };

        return kafkaProducer.send(record, callback);
    }

    public int publiserAlleBrukere() {
        val antallBrukere = repository.hentAntallBrukere().orElseThrow(IllegalStateException::new);
        val BATCH_SIZE = 1000;
        int offset = 0;

        do {
            List<OppfolgingKafkaDTO> brukere = repository.hentOppfolgingStatus(offset);
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
