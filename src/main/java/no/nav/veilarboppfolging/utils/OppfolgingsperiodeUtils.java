package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;

public class OppfolgingsperiodeUtils {

    public static Oppfolgingsperiode hentSisteOppfolgingsperiode(List<Oppfolgingsperiode> oppfolgingsperioder) {
        if (oppfolgingsperioder == null || oppfolgingsperioder.isEmpty()) {
            return null;
        }

        Oppfolgingsperiode oppfolgingsperiode = null;

        List<Oppfolgingsperiode> oppfolgingsperioderCopy = new ArrayList<>(oppfolgingsperioder);

        // Sorter start dato descending slik at riktig periode blir returnert hvis det finnes flere perioder med sluttDato == null
        // Burde egentlig ikke skje at det finnes flere uavsluttede perioder samtidig, men det har vært tilfeller hvor dette har skjedd
        oppfolgingsperioderCopy.sort(comparing(Oppfolgingsperiode::getStartDato).reversed());

        for (Oppfolgingsperiode periode : oppfolgingsperioderCopy) {
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
