package no.nav.veilarboppfolging.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarboppfolging.repository.KafkaProducerRepository;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.veilarboppfolging.utils.StringUtils.of;

@Service
@Slf4j
public class MetricsService {

    private final MetricsClient metricsClient;
    private final KafkaProducerRepository kafkaProducerRepository;
    private final MeterRegistry meterRegistry;

    private MetricsService(MetricsClient metricsClient, KafkaProducerRepository kafkaProducerRepository, MeterRegistry meterRegistry) {
        this.metricsClient = metricsClient;
        this.kafkaProducerRepository = kafkaProducerRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    private void init() {
        Gauge.builder("veilarboppfolging.kafka_producer.eldste_ubehandlet", kafkaProducerRepository, KafkaProducerRepository::getOldestMessage)
                .register(meterRegistry);
    }

    public VeilederTilordningEntity lestAvVeileder(VeilederTilordningEntity tilordning) {
        Event event = new Event("tilordnet.veileder.lest");

        Optional.of(tilordning)
                .map(VeilederTilordningEntity::getSistTilordnet)
                .map(MetricsService::msSiden)
                .ifPresent(ms -> event.addFieldToReport("ms", ms));

        metricsClient.report(event);

        return tilordning;
    }

    private static long msSiden(ZonedDateTime time){
        return ZonedDateTime.now().minusSeconds(time.toEpochSecond()).toInstant().toEpochMilli();
    }

    public void rapporterAutomatiskAvslutningAvOppfolging(boolean success) {
        Event event = new Event("oppfolging.automatisk.avslutning");
        event.addFieldToReport("success", success);
        metricsClient.report(event);
    }

    public void kvpStartet() {
        metricsClient.report(new Event("kvp.started"));
    }

    public void kvpStoppet() {
        metricsClient.report(new Event("kvp.stopped"));
    }

    public void stopKvpDueToChangedUnit() {
        metricsClient.report(new Event("kvp.stopped.byttet_enhet"));
    }

    public void oppdatertMittMal(MaalEntity malData, int antallMal) {
        String endretAv = malData.getEndretAvFormattert().toLowerCase();
        String bleOpprettet = antallMal == 1 ? "opprettet" : "endret";

        Event event = new Event("mittmal.oppdatering")
                .addFieldToReport("endretAv", endretAv + " " +  bleOpprettet);

        metricsClient.report(event);
    }

    public void antallBrukereAvsluttetAutomatisk(){
        metricsClient.report(new Event("automatisk.avsluttet.bruker"));
    }

    public void startetOppfolgingAutomatisk(String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        Event event = new Event("automatisk.startet.oppfolging.bruker")
                .addTagToReport("formidlingsgruppekode", of(formidlingsgruppekode).orElse("unknown"))
                .addTagToReport("kvalifiseringsgruppekode", of(kvalifiseringsgruppekode).orElse("unknown"));

        metricsClient.report(event);
    }

    public Integer getOldestUnprocessedKafkaProducerMessage() {
        log.info("Report oldest unprocessed message");
        Integer numOfMinutesSinceUnprocessed = kafkaProducerRepository.getOldestMessage();

        if (numOfMinutesSinceUnprocessed == null){
            numOfMinutesSinceUnprocessed = 0;
        }

        return numOfMinutesSinceUnprocessed;
    }
}
