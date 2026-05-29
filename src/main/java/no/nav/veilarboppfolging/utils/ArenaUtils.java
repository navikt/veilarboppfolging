package no.nav.veilarboppfolging.utils;

import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class ArenaUtils {

    private static final Formidlingsgruppe ARBS =  Formidlingsgruppe.ARBS;
    private static final Formidlingsgruppe ISERV = Formidlingsgruppe.ISERV;
    private static final Formidlingsgruppe IKKE_ARBEIDSSOKER = Formidlingsgruppe.IARBS;

    // kvalifiseringsgruppe = servicegruppe + innsatsgruppe
    protected static final Set<Kvalifiseringsgruppe> OPPFOLGING_KVALIFISERINGSGRUPPEKODER = new HashSet<>(asList(Kvalifiseringsgruppe.BATT, Kvalifiseringsgruppe.BFORM, Kvalifiseringsgruppe.IKVAL, Kvalifiseringsgruppe.VURDU, Kvalifiseringsgruppe.OPPFI, Kvalifiseringsgruppe.VARIG));

    public static boolean erIARBSUtenOppfolging(Formidlingsgruppe formidlingsgruppeKode, Kvalifiseringsgruppe kvalifiseringsgruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && !OPPFOLGING_KVALIFISERINGSGRUPPEKODER.contains(kvalifiseringsgruppeKode);
    }

    public static boolean erIserv(Formidlingsgruppe formidlingsgruppe) {
        return ISERV.equals(formidlingsgruppe);
    }

}
