package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.veilarboppfolging.config.KafkaProperties;
import no.nav.veilarboppfolging.domain.kafka.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

import static no.nav.common.kafka.producer.util.ProducerUtils.toJsonProducerRecord;

@Service
public class KafkaProducerService {

    private final AuthContextHolder authContextHolder;

    private final KafkaProducerRecordStorage<String, String> producerRecordStorage;

    private final KafkaProperties kafkaProperties;

    @Autowired
    public KafkaProducerService(AuthContextHolder authContextHolder,
                                KafkaProducerRecordStorage<String, String> producerRecordStorage,
                                KafkaProperties kafkaProperties) {
        this.authContextHolder = authContextHolder;
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
    }

    public void publiserEndringPaManuellStatus(String aktorId, boolean erManuell) {
        EndringPaManuellStatusKafkaDTO dto = new EndringPaManuellStatusKafkaDTO(aktorId, erManuell);
        store(kafkaProperties.getEndringPaManuellStatus(), dto.getAktorId(), dto);
    }

    public void publiserEndringPaNyForVeileder(String aktorId, boolean erNyForVeileder) {
        EndringPaNyForVeilederKafkaDTO dto = new EndringPaNyForVeilederKafkaDTO(aktorId, erNyForVeileder);
        store(kafkaProperties.getEndringPaNyForVeileder(), aktorId, dto);
    }

    public void publiserVeilederTilordnet(String aktorId, String tildeltVeilederId) {
        VeilederTilordnetKafkaDTO dto = new VeilederTilordnetKafkaDTO(aktorId, tildeltVeilederId);
        store(kafkaProperties.getVeilederTilordnet(), aktorId, dto);
    }

    public void publiserOppfolgingStartet(String aktorId) {
        OppfolgingStartetKafkaDTO dto = new OppfolgingStartetKafkaDTO(aktorId, ZonedDateTime.now());
        store(kafkaProperties.getOppfolgingStartet(), aktorId, dto);
    }

    public void publiserOppfolgingAvsluttet(String aktorId) {
        OppfolgingAvsluttetKafkaDTO dto = new OppfolgingAvsluttetKafkaDTO()
                .setAktorId(aktorId)
                .setSluttdato(ZonedDateTime.now());

        store(kafkaProperties.getOppfolgingAvsluttet(), aktorId, dto);

        // Deprecated
        store(kafkaProperties.getEndringPaaAvsluttOppfolging(), aktorId, dto);
    }

    public void publiserKvpStartet(String aktorId, String enhetId, String opprettetAvVeilederId, String begrunnelse) {
        KvpStartetKafkaDTO dto = new KvpStartetKafkaDTO()
                .setAktorId(aktorId)
                .setEnhetId(enhetId)
                .setOpprettetAv(opprettetAvVeilederId)
                .setOpprettetBegrunnelse(begrunnelse)
                .setOpprettetDato(ZonedDateTime.now());

        store(kafkaProperties.getKvpStartet(), aktorId, dto);
    }

    public void publiserKvpAvsluttet(String aktorId, String avsluttetAv, String begrunnelse) {
        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = new KvpAvsluttetKafkaDTO()
                .setAktorId(aktorId)
                .setAvsluttetAv(avsluttetAv) // veilederId eller System
                .setAvsluttetBegrunnelse(begrunnelse)
                .setAvsluttetDato(ZonedDateTime.now());

        store(kafkaProperties.getKvpAvlsuttet(), aktorId, kvpAvsluttetKafkaDTO);
    }

    public void publiserEndretMal(String aktorId, String veilederIdent){
        MalEndringKafkaDTO malEndringKafkaDTO = new MalEndringKafkaDTO()
                .setAktorId(aktorId)
                .setEndretTidspunk(ZonedDateTime.now())
                .setVeilederIdent(veilederIdent)
                .setLagtInnAv(authContextHolder.erEksternBruker()
                        ? MalEndringKafkaDTO.InnsenderData.BRUKER
                        : MalEndringKafkaDTO.InnsenderData.NAV);

        store(kafkaProperties.getEndringPaMal(), aktorId, malEndringKafkaDTO);
    }

    private void store(String topic, String key, Object value) {
        ProducerRecord<String, String> record = toJsonProducerRecord(topic, key, value);
        producerRecordStorage.store(record);
    }

}
