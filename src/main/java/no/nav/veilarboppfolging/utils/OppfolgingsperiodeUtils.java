package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;

import java.time.ZonedDateTime;
import java.util.List;

public class OppfolgingsperiodeUtils {

    public static Oppfolgingsperiode hentSisteOppfolgingsperiode(List<Oppfolgingsperiode> oppfolgingsperioder) {
        if (oppfolgingsperioder == null || oppfolgingsperioder.isEmpty()) {
            return null;
        }

        Oppfolgingsperiode oppfolgingsperiode = null;

        for (Oppfolgingsperiode periode : oppfolgingsperioder) {
            ZonedDateTime sluttDato = periode.getSluttDato();

            // Det skal maks finnes 1 periode som ikke har sluttDato (den gjeldende perioden)
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
