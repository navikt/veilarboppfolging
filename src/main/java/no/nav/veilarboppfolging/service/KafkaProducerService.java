package no.nav.veilarboppfolging.service;

import no.nav.common.auth.context.AuthContextHolder;
import no.nav.veilarboppfolging.domain.kafka.*;
import no.nav.veilarboppfolging.kafka.KafkaMessagePublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class KafkaProducerService {

    private final AuthContextHolder authContextHolder;

    private final KafkaMessagePublisher kafkaMessagePublisher;

    @Autowired
    public KafkaProducerService(AuthContextHolder authContextHolder, KafkaMessagePublisher kafkaMessagePublisher) {
        this.authContextHolder = authContextHolder;
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
                .setSluttdato(ZonedDateTime.now());

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

    public void publiserEndretMal(String aktorId, String veilederIdent){
        MalEndringKafkaDTO malEndringKafkaDTO = new MalEndringKafkaDTO()
                .setAktorId(aktorId)
                .setEndretTidspunk(ZonedDateTime.now())
                .setVeilederIdent(veilederIdent)
                .setLagtInnAv(authContextHolder.erEksternBruker()
                        ? MalEndringKafkaDTO.InnsenderData.BRUKER
                        : MalEndringKafkaDTO.InnsenderData.NAV);
        kafkaMessagePublisher.publiserEndringPaMal(malEndringKafkaDTO);
    }

}
