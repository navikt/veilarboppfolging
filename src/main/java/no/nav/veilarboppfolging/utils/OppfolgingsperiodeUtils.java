package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;

public class OppfolgingsperiodeUtils {

    public static OppfolgingsperiodeEntity hentSisteOppfolgingsperiode(List<OppfolgingsperiodeEntity> oppfolgingsperioder) {
        if (oppfolgingsperioder == null || oppfolgingsperioder.isEmpty()) {
            return null;
        }

        OppfolgingsperiodeEntity oppfolgingsperiode = null;

        List<OppfolgingsperiodeEntity> oppfolgingsperioderCopy = new ArrayList<>(oppfolgingsperioder);

        // Sorter start dato descending slik at riktig periode blir returnert hvis det finnes flere perioder med sluttDato == null
        // Burde egentlig ikke skje at det finnes flere uavsluttede perioder samtidig, men det har vært tilfeller hvor dette har skjedd
        oppfolgingsperioderCopy.sort(comparing(OppfolgingsperiodeEntity::getStartDato).reversed());

        for (OppfolgingsperiodeEntity periode : oppfolgingsperioderCopy) {
            ZonedDateTime sluttDato = periode.getSluttDato();

            if (sluttDato == null) {
                return periode;
            }

            if (oppfolgingsperiode == null || periode.getSluttDato().isAfter(oppfolgingsperiode.getSluttDato())) {
                oppfolgingsperiode = periode;
            }
        }

        return oppfolgingsperiode;
    }

}
