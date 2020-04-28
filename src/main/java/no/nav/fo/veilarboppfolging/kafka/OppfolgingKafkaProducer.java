package no.nav.fo.veilarboppfolging.kafka;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.utils.IdUtils;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingKafkaFeiletMeldingRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingKafkaDTO;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederTilordning;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
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
        runAsync(() -> send(aktoerId));
    }

    @SneakyThrows
    public Try<RecordMetadata> send(AktorId aktoerId) {
        log.info("Henter oppfølgingsstatus for bruker {}", aktoerId);

        val result = repository.hentOppfolgingStatus(aktoerId.getAktorId());
        result.onFailure(t -> log.error("Kunne ikke hente oppfølgingsstatus for bruker {} {}", aktoerId.getAktorId(), t));
        return result.flatMap(this::send);
    }

    private Try<RecordMetadata> send(OppfolgingKafkaDTO dto) {
        val aktoerId = dto.getAktoerid();
        val header = new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationIdAsBytes());
        val record = new ProducerRecord<>(topicName, 0, aktoerId, toJson(dto), singletonList(header));

        log.info("Legger ut bruker med aktoerId {} på topic {}", aktoerId, topicName);

        val result = Try.of(() -> kafkaProducer.send(record).get(10, SECONDS));
        result.onFailure(t -> {
            log.error("Kunne ikke sende melding på kafka for bruker {} {}", aktoerId, t);
            kafkaRepository.insertFeiletMelding(new AktorId(aktoerId));
        });

        kafkaRepository.deleteFeiletMelding(new AktorId(aktoerId));
        return result;
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
