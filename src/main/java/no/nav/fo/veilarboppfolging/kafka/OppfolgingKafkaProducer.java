package no.nav.fo.veilarboppfolging.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.utils.IdUtils;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingKafkaFeiletMeldingRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederTilordning;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.fo.veilarboppfolging.utils.FnrUtils.getAktorIdOrElseThrow;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
public class OppfolgingKafkaProducer {

    private final KafkaProducer<String, String> kafkaProducer;
    private final OppfolgingFeedRepository repository;
    private final OppfolgingKafkaFeiletMeldingRepository kafkaRepository;
    private final AktorService aktorService;
    private final String topicName;

    public OppfolgingKafkaProducer(KafkaProducer<String, String> kafkaProducer,
                                   OppfolgingFeedRepository repository,
                                   OppfolgingKafkaFeiletMeldingRepository kafkaRepository,
                                   AktorService aktorService, String topicName) {
        this.kafkaProducer = kafkaProducer;
        this.repository = repository;
        this.kafkaRepository = kafkaRepository;
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
        val future = runAsync(() -> send(aktoerId));
        future.exceptionally(e -> onError(aktoerId));
    }

    Void onError(AktorId aktoerId) {
        kafkaRepository.insertFeiletMelding(aktoerId);
        log.error("Kunne ikke publisere melding for bruker med aktoerId {} på topic {}", aktoerId, topicName);
        return null;
    }

    @SneakyThrows
    void send(AktorId aktoerId) {
        val dto = repository.hentOppfolgingStatus(aktoerId.getAktorId()).orElseThrow(IllegalStateException::new);
        val header = new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationIdAsBytes());
        val record = new ProducerRecord<>(topicName, 0, aktoerId.getAktorId(), toJson(dto), singletonList(header));

        kafkaProducer.send(record).get(10, SECONDS);
        kafkaRepository.deleteFeiletMelding(aktoerId);
        log.info("Bruker med aktoerId {} er lagt på topic {}", aktoerId, topicName);
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
