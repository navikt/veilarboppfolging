package no.nav.fo.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.utils.IdUtils;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingKafkaFeiletMeldingRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederTilordning;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getEnvironmentName;

@Slf4j
public class OppfolgingKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OppfolgingFeedRepository repository;
    private final OppfolgingKafkaFeiletMeldingRepository kafkaRepository;
    private final AktorService aktorService;

    private final static String TOPIC = "aapen-fo-oppfolgingOppdatert-v1-" + getEnvironmentName();

    public OppfolgingKafkaProducer(KafkaTemplate<String, String> kafkaTemplate,
                                   OppfolgingFeedRepository repository,
                                   OppfolgingKafkaFeiletMeldingRepository kafkaRepository,
                                   AktorService aktorService) {
        this.kafkaTemplate = kafkaTemplate;
        this.repository = repository;
        this.kafkaRepository = kafkaRepository;
        this.aktorService = aktorService;
    }

    public void sendAsync(List<VeilederTilordning> tilordninger) {
        for (VeilederTilordning tilordning : tilordninger) {
            val aktoerId = new AktorId(tilordning.getAktoerId());
            sendAsync(aktoerId);
        }
    }

    public void sendAsync(Fnr fnr) {
        CompletableFuture<AktorId> fetchAktoerId = supplyAsync(() -> FnrUtils.getAktorIdOrElseThrow(aktorService, fnr.getFnr()));
        fetchAktoerId.thenAccept(this::sendAsync);
    }

    public void sendAsync(AktorId aktorId) {
        supplyAsync(() -> send(aktorId));
    }

    public CompletableFuture<AktorId> send(AktorId aktoerId) {

        Optional<OppfolgingFeedDTO> dto = repository.hentOppfolgingStatus(aktoerId.getAktorId());
        if (!dto.isPresent()) {
            val feilMelding = String.format("Fant ikke oppfolgingsstatus for bruker %s", aktoerId);
            val failedFuture = new CompletableFuture<AktorId>();
            failedFuture.completeExceptionally(new RuntimeException(feilMelding));

            log.error(feilMelding);
            return failedFuture;
        }

        val header = new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationIdAsBytes());
        val record = new ProducerRecord<>(TOPIC, 0, aktoerId.getAktorId(), toJson(dto), singletonList(header));

        val springFuture = kafkaTemplate.send(record);
        springFuture.addCallback(onSuccess(aktoerId), onError(aktoerId));

        return springFuture
                .completable()
                .thenApply(result -> result.getProducerRecord().key())
                .thenApply(AktorId::new);
    }

    FailureCallback onError(AktorId aktoerId) {
        return error -> {
            String message = "Kunne ikke publisere melding for bruker med aktoerId {} på topic {}";
            log.error(message, aktoerId, TOPIC);
            kafkaRepository.insertFeiletMelding(aktoerId);
        };
    }

    SuccessCallback<SendResult<String, String>> onSuccess(AktorId aktoerId) {
        return success -> {
            String message = "Bruker med aktoerId {} er lagt på topic {}";
            log.info(message, aktoerId, TOPIC);
            kafkaRepository.deleteFeiletMelding(aktoerId);
        };
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
