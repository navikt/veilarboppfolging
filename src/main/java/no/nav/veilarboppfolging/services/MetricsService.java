package no.nav.veilarboppfolging.services;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.domain.Tilordning;

import static no.nav.veilarboppfolging.utils.StringUtils.of;

import java.util.Date;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MetricsClient metricsClient;

    private MetricsService(MetricsClient metricsClient) {
        this.metricsClient = metricsClient;
    }

    public void report(Event event) {
        metricsClient.report(event);
    }

    public Tilordning lestAvVeileder(Tilordning tilordning) {
        Event event = new Event("tilordnet.veileder.lest");

        Optional.of(tilordning)
                .map(Tilordning::getSistTilordnet)
                .map(Date::getTime)
                .map(MetricsService::msSiden)
                .ifPresent(ms -> event.addFieldToReport("ms", ms));

        metricsClient.report(event);

        return tilordning;
    }

    private static long msSiden(long time){
        return new Date().getTime() - time;
    }

    public void raporterAutomatiskAvslutningAvOppfolging(boolean success) {
        Event event = new Event("oppfolging.automatisk.avslutning");
        event.addFieldToReport("success", success);
        metricsClient.report(event);
    }

    public void startKvp() {
        metricsClient.report(new Event("kvp.started"));
    }

    public void stopKvp() {
        metricsClient.report(new Event("kvp.stopped"));
    }

    public void stopKvpDueToChangedUnit() {
        metricsClient.report(new Event("kvp.stopped.byttet_enhet"));
    }

    public void oppdatertMittMal(MalData malData, int antallMal) {
        String endretAv = malData.getEndretAvFormattert().toLowerCase();
        String bleOpprettet = antallMal == 1 ? "opprettet" : "endret";

        Event event = new Event("mittmal.oppdatering")
                .addFieldToReport("endretAv", endretAv + " " +  bleOpprettet);

        metricsClient.report(event);
    }

    public void antallMeldingerKonsumertAvKafka() {
        metricsClient.report(new Event("kafka.konsumert.meldinger"));
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

}
