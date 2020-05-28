package no.nav.fo.veilarboppfolging.kafka;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.Oppfolging;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.fo.veilarboppfolging.utils.DateUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.fo.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer.MeldingsType.*;
import static no.nav.json.JsonUtils.toJson;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
public class OppfolgingStatusKafkaProducer {

    private final KafkaProducer<String, String> kafka;
    private final OppfolgingFeedRepository repository;
    private final String topicName;


    @Value
    @Builder
    public static class Melding {
        String aktoerid;
        String veileder;
        boolean oppfolging;
        Boolean nyForVeileder;
        Boolean manuell;
        ZonedDateTime startDato;
        MeldingsType meldingsType;
    }

    enum MeldingsType {
        OPPFOLGING_STARTET,
        OPPFOLGING_AVSLUTTET,
        ENDRING_I_MANUELL_STATUS,
        VEILEDER_TILORDNET,
        AKTVITETSPLAN_LEST_AV_VEILEDER
    }

    public OppfolgingStatusKafkaProducer(KafkaProducer<String, String> kafka,
                                         OppfolgingFeedRepository repository,
                                         String topicName) {
        this.kafka = kafka;
        this.repository = repository;
        this.topicName = topicName;
    }


    @SneakyThrows
    public RecordMetadata send(Melding melding) {
        val aktoerId = melding.getAktoerid();
        val header = new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationIdAsBytes());
        val record = new ProducerRecord<>(topicName, 0, aktoerId, toJson(melding), singletonList(header));

        log.info("Sender {} for bruker {}", melding.getMeldingsType(), melding.getAktoerid());
        return kafka.send(record).get();
    }

    public void send(String aktorId) {
        repository
                .hentOppfolgingStatus(aktorId)
                .onSuccess(this::send)
                .onFailure(e -> log.error("Kunne ikke sende oppfølgingsstatus for bruker {}", aktorId));
    }

    public int publiserAlleBrukere() {
        val antallBrukere = repository.hentAntallBrukere().orElseThrow(IllegalStateException::new);
        val BATCH_SIZE = 1000;
        int offset = 0;

        do {
            List<Melding> brukere = repository.hentOppfolgingStatus(offset);
            brukere.forEach(this::send);
            offset = offset + BATCH_SIZE;
        }
        while (offset <= antallBrukere);

        return offset;
    }

    public void sendStartOppfolging(Oppfolging oppfolging) {
        Melding melding = build(oppfolging)
                .nyForVeileder(false)
                .meldingsType(OPPFOLGING_STARTET)
                .build();

        send(melding);
    }

    public void sendAvsluttOppfolging(Oppfolging oppfolging) {
        Melding melding = build(oppfolging)
                .nyForVeileder(false)
                .meldingsType(OPPFOLGING_AVSLUTTET)
                .build();

        send(melding);
    }

    public void sendOpppdaterManuellStatus(Oppfolging oppfolging) {
        Melding melding = build(oppfolging)
                .nyForVeileder(null)
                .meldingsType(ENDRING_I_MANUELL_STATUS)
                .build();

        send(melding);
    }


    public void sendAktivitetsplanLestAvVeileder(String aktorId) {
        Melding melding = Melding.builder()
                .aktoerid(aktorId)
                .nyForVeileder(false)
                .meldingsType(AKTVITETSPLAN_LEST_AV_VEILEDER)
                .build();

        send(melding);
    }

    public void sendVeilederTilordning(String aktoerId, String veileder) {
        Melding melding = Melding.builder()
                .aktoerid(aktoerId)
                .veileder(veileder)
                .oppfolging(true)
                .nyForVeileder(true)
                .meldingsType(VEILEDER_TILORDNET)
                .build();

        send(melding);
    }


    private static Melding.MeldingBuilder build(Oppfolging oppfolging) {
        return Melding.builder()
                .aktoerid(oppfolging.getAktorId())
                .veileder(oppfolging.getVeilederId())
                .oppfolging(oppfolging.isUnderOppfolging())
                .manuell(oppfolging.getGjeldendeManuellStatus().isManuell())
                .startDato(hentStartDato(oppfolging));
    }

    private static ZonedDateTime hentStartDato(Oppfolging oppfolging) {
        return oppfolging.getOppfolgingsperioder().stream()
                .sorted(Comparator.comparing(Oppfolgingsperiode::getStartDato))
                .findFirst()
                .map(Oppfolgingsperiode::getStartDato)
                .map(DateUtils::toZonedDateTime)
                .orElse(null);
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
