package no.nav.fo.veilarboppfolging.kafka;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.utils.IdUtils;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
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

import static java.util.Collections.singletonList;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getEnvironmentName;

@Slf4j
public class OppfolgingKafkaProducer {

    private final KafkaTemplate<String, String> kafka;
    private final OppfolgingFeedRepository repository;
    private final AktorService aktorService;

    private final static String TOPIC = "aapen-fo-oppfolgingOppdatert-v1-" + getEnvironmentName();

    public OppfolgingKafkaProducer(KafkaTemplate<String, String> kafka, OppfolgingFeedRepository repository, AktorService aktorService) {
        this.kafka = kafka;
        this.repository = repository;
        this.aktorService = aktorService;
    }

    public void send(List<VeilederTilordning> tilordninger) {
        for (VeilederTilordning tilordning : tilordninger) {
            val aktoerId = new AktorId(tilordning.getAktoerId());
            sendForAktoerId(aktoerId);
        }
    }

    public void send(String fnr) {
        AktorId aktoerId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);
        sendForAktoerId(aktoerId);
    }

    private void sendForAktoerId(AktorId aktoerId) {
        byte[] correlationId = getCorrelationIdAsBytes();

        Optional<OppfolgingFeedDTO> dto = repository.hentOppfolgingStatus(aktoerId.getAktorId());
        if (!dto.isPresent()) {
            log.error("Fant ikke oppfolgingsstatus for bruker {}", aktoerId);
            return;
        }

        val header = new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId);
        val record = new ProducerRecord<>(TOPIC, 0, aktoerId.getAktorId(), toJson(dto), singletonList(header));

        kafka.send(record).addCallback(onSuccess(aktoerId), onError(aktoerId));
    }

    private FailureCallback onError(AktorId aktoerId) {
        return error -> {
            String message = "Kunne ikke publisere melding for bruker med aktoerId {} på topic {}";
            log.error(message, aktoerId, TOPIC);
        };
    }

    private SuccessCallback<SendResult<String, String>> onSuccess(AktorId aktoerId) {
        return success -> {
            String message = "Bruker med aktoerId {} er lagt på topic {}";
            log.info(message, aktoerId, TOPIC);
        };
    }

    private static byte[] getCorrelationIdAsBytes() {
        try {
            return MDC.get(PREFERRED_NAV_CALL_ID_HEADER_NAME).getBytes();
        } catch (IllegalArgumentException e) {
            return IdUtils.generateId().getBytes();
        }
    }
}
