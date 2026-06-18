package no.nav.veilarboppfolging.utils;

import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import no.nav.pto_schema.kafka.json.topic.SisteTilordnetVeilederV1;
import no.nav.veilarboppfolging.controller.response.*;
import no.nav.veilarboppfolging.domain.AvslutningStatusData;
import no.nav.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.veilarboppfolging.kafka.dto.OppfolgingsperiodeDTO;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.entity.MaalEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.repository.entity.VeilederTilordningEntity;

import java.util.Collections;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class DtoMappers {

    public static Maal tilDto(MaalEntity malData) {
        return new Maal(
            malData.getMal(),
            malData.getEndretAvFormattert(),
            malData.getDato()
        );
    }

    public static Maal tilDto(Optional<MaalEntity> malData) {
        if (malData.isPresent()) {
            return tilDto(malData.get());
        } else {
            return new Maal(null, null, null);
        }
    }

    /**
     * Given a Kvp object, return its DTO representation. All fields are included.
     */
    public static KvpDTO kvpToDTO(KvpPeriodeEntity k) {
        return new KvpDTO(
                k.getKvpId(),
                k.getSerial(),
                k.getAktorId(),
                k.getEnhet(),
                k.getOpprettetAv(),
                k.getOpprettetDato(),
                k.getOpprettetBegrunnelse(),
                k.getAvsluttetAv(),
                k.getAvsluttetDato(),
                k.getAvsluttetBegrunnelse()
        );
    }

    public static AvslutningsStatusDto tilDto(AvslutningStatusData avslutningStatusData) {
        return new AvslutningsStatusDto(
                avslutningStatusData.getKanAvslutte(),
                avslutningStatusData.getUnderOppfolging(),
                avslutningStatusData.getHarYtelser(),
                avslutningStatusData.getUnderKvp(),
                avslutningStatusData.getInaktiveringsDato(),
                avslutningStatusData.getErIserv(),
                avslutningStatusData.getHarAktiveTiltaksdeltakelser(),
                avslutningStatusData.getErDeltakerIUngdomsprogrammet(),
                avslutningStatusData.getErArbeidssoeker(),
                avslutningStatusData.getHarAap()
        );
    }

    public static OppfolgingStatus tilDto(OppfolgingStatusData oppfolgingStatusData, boolean erInternBruker) {
        return new OppfolgingStatus(
                oppfolgingStatusData.getFnr(),
                oppfolgingStatusData.getAktorId(),
                erInternBruker ? oppfolgingStatusData.getVeilederId() : null,
                oppfolgingStatusData.getReservasjonKRR(),
                oppfolgingStatusData.getRegistrertKRR(),
                oppfolgingStatusData.getKanVarsles(),
                oppfolgingStatusData.getManuell(),
                oppfolgingStatusData.getUnderOppfolging(),
                oppfolgingStatusData.getUnderKvp(),
                oppfolgingStatusData.getOppfolgingUtgang(),
                erInternBruker ? oppfolgingStatusData.getKanStarteOppfolging() : true,
                null,
                oppfolgingStatusData.getOppfolgingsperioder().stream().map(o -> tilOppfolgingPeriodeDTO(o, erInternBruker)).collect(toList()),
                erInternBruker ? oppfolgingStatusData.getHarSkriveTilgang() : null,
                erInternBruker ? oppfolgingStatusData.getInaktivIArena() : null,
                oppfolgingStatusData.getKanReaktiveres(),
                oppfolgingStatusData.getInaktiveringsdato(),
                oppfolgingStatusData.getErSykmeldtMedArbeidsgiver(),
                oppfolgingStatusData.getServicegruppe(),
                oppfolgingStatusData.getFormidlingsgruppe(),
                oppfolgingStatusData.getRettighetsgruppe()
        );
    }

    public static OppfolgingPeriodeDTO tilOppfolgingPeriodeDTO(OppfolgingsperiodeEntity oppfolgingsperiode, boolean erInternBruker) {
        return new OppfolgingPeriodeDTO(
                oppfolgingsperiode.getUuid(),
                erInternBruker ? oppfolgingsperiode.getAktorId() : null,
                erInternBruker ? oppfolgingsperiode.getAvsluttetAv() : null,
                oppfolgingsperiode.getStartDato(),
                oppfolgingsperiode.getSluttDato(),
                erInternBruker ? oppfolgingsperiode.getBegrunnelse() : null,
                Optional.of(oppfolgingsperiode.getKvpPerioder()).orElseGet(Collections::emptyList)
                        .stream().map(DtoMappers::tilDTO).collect(toList())
        );
    }

    public static SisteTilordnetVeilederV1 tilSisteTilordnetVeilederKafkaDTO(VeilederTilordningEntity tilordning) {
        return new SisteTilordnetVeilederV1(
                tilordning.getAktorId(),
                tilordning.getVeilederId(),
                tilordning.getSistOppdatert()
        );
    }

    public static SisteOppfolgingsperiodeV1 tilSisteOppfolgingsperiodeV1(OppfolgingsperiodeEntity oppfolgingsperiode) {
        return new SisteOppfolgingsperiodeV1(
                oppfolgingsperiode.getUuid(),
                oppfolgingsperiode.getAktorId(),
                oppfolgingsperiode.getStartDato(),
                oppfolgingsperiode.getSluttDato()
        );
    }

    public static OppfolgingsperiodeDTO tilOppfolgingsperiodeDTO(OppfolgingsperiodeEntity oppfolgingsperiode) {
        return new OppfolgingsperiodeDTO(
                oppfolgingsperiode.getUuid().toString(),
                oppfolgingsperiode.getStartDato(),
                oppfolgingsperiode.getSluttDato(),
                oppfolgingsperiode.getAktorId(),
                Optional.ofNullable(oppfolgingsperiode.getStartetBegrunnelse()).map(b -> b.toStartetBegrunnelseDTO()).orElse(null)
        );
    }

    public static OppfolgingPeriodeMinimalDTO tilOppfolgingPeriodeMinimalDTO(OppfolgingsperiodeEntity oppfolgingsperiode) {
        return new OppfolgingPeriodeMinimalDTO(
                oppfolgingsperiode.getUuid(),
                oppfolgingsperiode.getStartDato(),
                oppfolgingsperiode.getSluttDato()
        );
    }

    public static KvpPeriodeDTO tilDTO(KvpPeriodeEntity kvp) {
        return new KvpPeriodeDTO(kvp.getOpprettetDato(), kvp.getAvsluttetDato());
    }

}
