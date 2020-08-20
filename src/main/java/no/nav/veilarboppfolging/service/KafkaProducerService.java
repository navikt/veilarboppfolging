package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.kafka.AvsluttOppfolgingKafkaDTO;
import no.nav.veilarboppfolging.domain.kafka.KvpEndringKafkaDTO;
import no.nav.veilarboppfolging.domain.kafka.OppfolgingKafkaDTO;
import no.nav.veilarboppfolging.domain.kafka.OppfolgingStartetKafkaDTO;
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

    public void publiserOppfolgingStatusEndret(String aktorId) {
        OppfolgingKafkaDTO oppfolgingKafkaDTO = oppfolgingFeedRepository.hentOppfolgingStatus(aktorId);
        kafkaMessagePublisher.publiserOppfolgingStatusEndret(oppfolgingKafkaDTO);
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
