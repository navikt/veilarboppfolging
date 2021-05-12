package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.controller.domain.*;
import no.nav.veilarboppfolging.domain.*;

import java.util.Collections;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class DtoMappers {

    public static Mal tilDto(MalData malData) {
        return new Mal()
                .setMal(malData.getMal())
                .setEndretAv(malData.getEndretAvFormattert())
                .setDato(malData.getDato());
    }

    /**
     * Given a Kvp object, return its DTO representation. All fields are included.
     */
    public static KvpDTO kvpToDTO(Kvp k) {
        return new KvpDTO()
                .setKvpId(k.getKvpId())
                .setSerial(k.getSerial())
                .setAktorId(k.getAktorId())
                .setAvsluttetAv(k.getAvsluttetAv())
                .setAvsluttetBegrunnelse(k.getAvsluttetBegrunnelse())
                .setAvsluttetDato(k.getAvsluttetDato())
                .setEnhet(k.getEnhet())
                .setOpprettetAv(k.getOpprettetAv())
                .setOpprettetBegrunnelse(k.getOpprettetBegrunnelse())
                .setOpprettetDato(k.getOpprettetDato());
    }

    public static AvslutningStatus tilDto(AvslutningStatusData avslutningStatusData) {
        return new AvslutningStatus(
                avslutningStatusData.kanAvslutte,
                avslutningStatusData.underOppfolging,
                avslutningStatusData.harYtelser,
                avslutningStatusData.underKvp,
                avslutningStatusData.inaktiveringsDato
        );
    }

    public static OppfolgingStatus tilDto(OppfolgingStatusData oppfolgingStatusData, boolean erInternBruker) {
        OppfolgingStatus status = new OppfolgingStatus()
                .setFnr(oppfolgingStatusData.fnr)
                .setAktorId(oppfolgingStatusData.aktorId)
                .setUnderOppfolging(oppfolgingStatusData.underOppfolging)
                .setManuell(oppfolgingStatusData.manuell)
                .setReservasjonKRR(oppfolgingStatusData.reservasjonKRR)
                .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                .setKanReaktiveres(oppfolgingStatusData.kanReaktiveres)
                .setOppfolgingsPerioder(oppfolgingStatusData.oppfolgingsperioder.stream().map(o -> tilOppfolgingPeriodeDTO(o, erInternBruker)).collect(toList()))
                .setInaktiveringsdato(oppfolgingStatusData.inaktiveringsdato)
                .setGjeldendeEskaleringsvarsel(tilDto(oppfolgingStatusData.getGjeldendeEskaleringsvarsel(), erInternBruker))
                .setErIkkeArbeidssokerUtenOppfolging(oppfolgingStatusData.getErSykmeldtMedArbeidsgiver())
                .setErSykmeldtMedArbeidsgiver(oppfolgingStatusData.getErSykmeldtMedArbeidsgiver())
                .setHarSkriveTilgang(true)
                .setServicegruppe(oppfolgingStatusData.getServicegruppe())
                .setFormidlingsgruppe(oppfolgingStatusData.getFormidlingsgruppe())
                .setRettighetsgruppe(oppfolgingStatusData.getRettighetsgruppe())
                .setKanVarsles(oppfolgingStatusData.kanVarsles)
                .setUnderKvp(oppfolgingStatusData.underKvp);

        if (erInternBruker) {
            status
                    .setVeilederId(oppfolgingStatusData.veilederId)
                    .setKanStarteOppfolging(oppfolgingStatusData.isKanStarteOppfolging())
                    .setOppfolgingUtgang(oppfolgingStatusData.getOppfolgingUtgang())
                    .setHarSkriveTilgang(oppfolgingStatusData.harSkriveTilgang)
                    .setInaktivIArena(oppfolgingStatusData.inaktivIArena);
        }

        return status;
    }

    public static OppfolgingPeriodeDTO tilOppfolgingPeriodeDTO(Oppfolgingsperiode oppfolgingsperiode, boolean erInternBruker) {
        OppfolgingPeriodeDTO periode = new OppfolgingPeriodeDTO()
                .setUuid(oppfolgingsperiode.getUuid())
                .setSluttDato(oppfolgingsperiode.getSluttDato())
                .setStartDato(oppfolgingsperiode.getStartDato())
                .setKvpPerioder(
                        ofNullable(oppfolgingsperiode.getKvpPerioder()).orElseGet(Collections::emptyList)
                                .stream().map(DtoMappers::tilDTO).collect(toList())
                );

        if (erInternBruker) {
            periode.setVeileder(oppfolgingsperiode.getVeileder())
                    .setAktorId(oppfolgingsperiode.getAktorId())
                    .setBegrunnelse(oppfolgingsperiode.getBegrunnelse());
        }

        return periode;
    }

    public static OppfolgingPeriodeMinimalDTO tilOppfolgingPeriodeMinimalDTO(Oppfolgingsperiode oppfolgingsperiode, boolean erInternBruker) {
        return new OppfolgingPeriodeMinimalDTO()
                .setSluttDato(oppfolgingsperiode.getSluttDato())
                .setStartDato(oppfolgingsperiode.getStartDato());

    }

    public static KvpPeriodeDTO tilDTO(Kvp kvp) {
        return new KvpPeriodeDTO(kvp.getOpprettetDato(), kvp.getAvsluttetDato());
    }

    public static Eskaleringsvarsel tilDto(EskaleringsvarselData eskaleringsvarselData, boolean erInternBruker) {
        return Optional.ofNullable(eskaleringsvarselData)
                .map(eskalering -> Eskaleringsvarsel.builder()
                        .varselId(eskalering.getVarselId())
                        .aktorId(eskalering.getAktorId())
                        .opprettetAv(erInternBruker ? eskalering.getOpprettetAv() : null)
                        .opprettetDato(eskalering.getOpprettetDato())
                        .avsluttetDato(eskalering.getAvsluttetDato())
                        .tilhorendeDialogId(eskalering.getTilhorendeDialogId())
                        .build()
                ).orElse(null);
    }

}
