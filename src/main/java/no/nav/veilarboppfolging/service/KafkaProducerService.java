package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import no.nav.pto_schema.kafka.json.topic.SisteTilordnetVeilederV1;
import no.nav.pto_schema.kafka.json.topic.onprem.*;
import no.nav.veilarboppfolging.config.KafkaProperties;
import no.nav.veilarboppfolging.kafka.AoMinSideMicrofrontendMessage;
import no.nav.veilarboppfolging.kafka.KvpPeriode;
import no.nav.veilarboppfolging.kafka.dto.OppfolgingsperiodeDTO;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

import static no.nav.common.kafka.producer.util.ProducerUtils.serializeJsonRecord;

@Slf4j
@Service
public class KafkaProducerService {

    private final AuthContextHolder authContextHolder;

    private final KafkaProducerRecordStorage producerRecordStorage;

    private final KafkaProperties kafkaProperties;

    private final Boolean kafkaEnabled;

    private final AuthService authService;

    @Autowired
    public KafkaProducerService(
            AuthContextHolder authContextHolder,
            KafkaProducerRecordStorage producerRecordStorage,
            KafkaProperties kafkaProperties,
            @Value("${app.kafka.enabled}") Boolean kafkaEnabled, AuthService authService
    ) {
        this.authContextHolder = authContextHolder;
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
        this.kafkaEnabled = kafkaEnabled;
        this.authService = authService;
    }

    public void publiserOppfolgingsperiode(OppfolgingsperiodeDTO oppfolgingsperiode) {
        sisteOppfolgingsPeriode(oppfolgingsperiode.toSisteOppfolgingsperiodeDTO());
        oppfolingsperiode(oppfolgingsperiode);
    }

    private void sisteOppfolgingsPeriode(SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1) {
        store(kafkaProperties.getSisteOppfolgingsperiodeTopic(),
                sisteOppfolgingsperiodeV1.getAktorId(),
                sisteOppfolgingsperiodeV1);
    }

    private void oppfolingsperiode(OppfolgingsperiodeDTO sisteOppfolgingsperiodeV1) {
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

    public void publiserKvpStartet(AktorId aktorId, String enhetId, String opprettetAvVeilederId, String begrunnelse, ZonedDateTime startDato) {
        KvpStartetV1 recordValue = KvpStartetV1.builder()
                .aktorId(aktorId.get())
                .enhetId(enhetId)
                .opprettetAv(opprettetAvVeilederId)
                .opprettetBegrunnelse(begrunnelse)
                .opprettetDato(startDato)
                .build();

        store(kafkaProperties.getKvpStartetTopicAiven(), aktorId.get(), recordValue);
    }

    public void publiserKvpAvsluttet(AktorId aktorId, String avsluttetAv, String begrunnelse, ZonedDateTime sluttDato) {
        KvpAvsluttetV1 recordValue = KvpAvsluttetV1.builder()
                .aktorId(aktorId.get())
                .avsluttetAv(avsluttetAv) // veilederId eller System
                .avsluttetBegrunnelse(begrunnelse)
                .avsluttetDato(sluttDato)
                .build();

        store(kafkaProperties.getKvpAvsluttetTopicAiven(), aktorId.get(), recordValue);
    }

    public void publiserKvpPeriode(KvpPeriode kvpPeriode) {
        store(kafkaProperties.getKvpPerioderTopicAiven(), kvpPeriode.getAktorId(), kvpPeriode);
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

        store(kafkaProperties.getEndringPaMalAiven(), aktorId.get(), recordValue);
    }

    public void publiserVisAoMinSideMicrofrontend(AktorId aktorId) {
        Fnr fnr = authService.getFnrOrThrow(aktorId);

        AoMinSideMicrofrontendMessage message = new AoMinSideMicrofrontendMessage("enable", fnr.get(), "idporten-loa-substantial");

        log.info("Oppfølging startet for bruker - publiserer enable-melding på min-side-microfrontend-topic. Melding: {}", message);
        store(kafkaProperties.getMinSideAapenMicrofrontendV1(), aktorId.get(), message);
    }

    public void publiserSkjulAoMinSideMicrofrontend(AktorId aktorId) {
        Fnr fnr = authService.getFnrOrThrow(aktorId);

        AoMinSideMicrofrontendMessage message = new AoMinSideMicrofrontendMessage("disable", fnr.get());

        log.info("Oppfølging startet for bruker - publiserer disable-melding på min-side-microfrontend-topic. Melding: {}", message);
        store(kafkaProperties.getMinSideAapenMicrofrontendV1(), aktorId.get(), message);
    }

    private void store(String topic, String key, Object value) {
        if (kafkaEnabled) {
            ProducerRecord<byte[], byte[]> record = serializeJsonRecord(new ProducerRecord<>(topic, key, value));
            producerRecordStorage.store(record);
        } else {
            throw new RuntimeException("Kafka er disabled, men noe gjør at man forsøker å publisere meldinger");
        }
    }

}
