package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.kafka.*;
import no.nav.veilarboppfolging.kafka.KafkaMessagePublisher;
import no.nav.veilarboppfolging.repository.OppfolgingFeedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Service
public class KafkaProducerService {

    private final KafkaMessagePublisher kafkaMessagePublisher;

    private final OppfolgingFeedRepository oppfolgingFeedRepository;

    @Autowired
    public KafkaProducerService(KafkaMessagePublisher kafkaMessagePublisher, OppfolgingFeedRepository oppfolgingFeedRepository) {
        this.kafkaMessagePublisher = kafkaMessagePublisher;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
    }

    public void publiserEndringPaManuellStatus(String aktorId, boolean erManuell) {
        EndringPaManuellStatusKafkaDTO endringPaManuellStatusKafkaDTO = new EndringPaManuellStatusKafkaDTO(aktorId, erManuell);
        kafkaMessagePublisher.publiserEndringPaManuellStatus(endringPaManuellStatusKafkaDTO);
    }

    public void publiserNyForVeileder(String aktorId, boolean erNyForVeileder) {
        EndringPaNyForVeilederKafkaDTO endringPaManuellStatusKafkaDTO = new EndringPaNyForVeilederKafkaDTO(aktorId, erNyForVeileder);
        kafkaMessagePublisher.publiserNyForVeileder(endringPaManuellStatusKafkaDTO);
    }

    public void publiserVeilederTilordnet(String aktorId, String tildeltVeilederId) {
        VeilederTilordnetKafkaDTO veilederTilordnetKafkaDTO = new VeilederTilordnetKafkaDTO(aktorId, tildeltVeilederId);
        kafkaMessagePublisher.publiserVeilederTilordnet(veilederTilordnetKafkaDTO);
    }

    public void publiserOppfolgingStartet(String aktorId) {
        kafkaMessagePublisher.publiserOppfolgingStartet(new OppfolgingStartetKafkaDTO(aktorId, ZonedDateTime.now()));
    }

    public void publiserOppfolgingAvsluttet(String aktorId) {
        AvsluttOppfolgingKafkaDTO avsluttOppfolgingKafkaDTO = new AvsluttOppfolgingKafkaDTO()
                .setAktorId(aktorId)
                .setSluttdato(LocalDateTime.now());

        kafkaMessagePublisher.publiserOppfolgingAvsluttet(avsluttOppfolgingKafkaDTO);
    }

    public void publiserKvpStartet(String aktorId, String enhetId, String opprettetAvVeilederId, String begrunnelse) {
        KvpEndringKafkaDTO kvpEndring = new KvpEndringKafkaDTO()
                .setAktorId(aktorId)
                .setEnhetId(enhetId)
                .setOpprettetAv(opprettetAvVeilederId)
                .setOpprettetBegrunnelse(begrunnelse)
                .setOpprettetDato(ZonedDateTime.now());

        kafkaMessagePublisher.publiserKvpEndring(kvpEndring);
    }

    public void publiserKvpAvsluttet(String aktorId, String enhetId, String avsluttetAvVeilederId, String begrunnelse) {
        KvpEndringKafkaDTO kvpEndring = new KvpEndringKafkaDTO()
                .setAktorId(aktorId)
                .setEnhetId(enhetId)
                .setAvsluttetAv(avsluttetAvVeilederId)
                .setAvsluttetBegrunnelse(begrunnelse)
                .setAvsluttetDato(ZonedDateTime.now());

        kafkaMessagePublisher.publiserKvpEndring(kvpEndring);
    }

}
