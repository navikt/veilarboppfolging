package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.kafka.*;
import no.nav.veilarboppfolging.kafka.KafkaMessagePublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Service
public class KafkaProducerService {

    private final KafkaMessagePublisher kafkaMessagePublisher;

    @Autowired
    public KafkaProducerService(KafkaMessagePublisher kafkaMessagePublisher) {
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    public void publiserEndringPaManuellStatus(String aktorId, boolean erManuell) {
        EndringPaManuellStatusKafkaDTO endringPaManuellStatusKafkaDTO = new EndringPaManuellStatusKafkaDTO(aktorId, erManuell);
        kafkaMessagePublisher.publiserEndringPaManuellStatus(endringPaManuellStatusKafkaDTO);
    }

    public void publiserEndringPaNyForVeileder(String aktorId, boolean erNyForVeileder) {
        EndringPaNyForVeilederKafkaDTO endringPaManuellStatusKafkaDTO = new EndringPaNyForVeilederKafkaDTO(aktorId, erNyForVeileder);
        kafkaMessagePublisher.publiserEndringPaNyForVeileder(endringPaManuellStatusKafkaDTO);
    }

    public void publiserVeilederTilordnet(String aktorId, String tildeltVeilederId) {
        VeilederTilordnetKafkaDTO veilederTilordnetKafkaDTO = new VeilederTilordnetKafkaDTO(aktorId, tildeltVeilederId);
        kafkaMessagePublisher.publiserVeilederTilordnet(veilederTilordnetKafkaDTO);
    }

    public void publiserOppfolgingStartet(String aktorId) {
        kafkaMessagePublisher.publiserOppfolgingStartet(new OppfolgingStartetKafkaDTO(aktorId, ZonedDateTime.now()));
    }

    public void publiserOppfolgingAvsluttet(String aktorId) {
        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO = new OppfolgingAvsluttetKafkaDTO()
                .setAktorId(aktorId)
                .setSluttdato(LocalDateTime.now());

        kafkaMessagePublisher.publiserOppfolgingAvsluttet(oppfolgingAvsluttetKafkaDTO);
        kafkaMessagePublisher.publiserEndringPaAvsluttOppfolging(oppfolgingAvsluttetKafkaDTO);
    }

    public void publiserKvpStartet(String aktorId, String enhetId, String opprettetAvVeilederId, String begrunnelse) {
        KvpStartetKafkaDTO kvpStartetKafkaDTO = new KvpStartetKafkaDTO()
                .setAktorId(aktorId)
                .setEnhetId(enhetId)
                .setOpprettetAv(opprettetAvVeilederId)
                .setOpprettetBegrunnelse(begrunnelse)
                .setOpprettetDato(ZonedDateTime.now());

        kafkaMessagePublisher.publiserKvpStartet(kvpStartetKafkaDTO);
    }

    public void publiserKvpAvsluttet(String aktorId, String avsluttetAv, String begrunnelse) {
        KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = new KvpAvsluttetKafkaDTO()
                .setAktorId(aktorId)
                .setAvsluttetAv(avsluttetAv) // veilederId eller System
                .setAvsluttetBegrunnelse(begrunnelse)
                .setAvsluttetDato(ZonedDateTime.now());

        kafkaMessagePublisher.publiserKvpAvsluttet(kvpAvsluttetKafkaDTO);
    }

}
