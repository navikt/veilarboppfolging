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

    // Logikken som utleder om en bruker er under oppfolging kjøres også ved indeksering av brukere i VeilArbPortefølje.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(Formidlingsgruppe formidlingsgruppeKode, Kvalifiseringsgruppe kvalifiseringsgruppeKode) {
        return erArbeidssoker(formidlingsgruppeKode) || erIARBSMedOppfolging(formidlingsgruppeKode, kvalifiseringsgruppeKode);
    }

    public static boolean kanSettesUnderOppfolging(Formidlingsgruppe formidlingsgruppeKode, Kvalifiseringsgruppe kvalifiseringsgruppeKode) {
        return erIARBSUtenOppfolging(formidlingsgruppeKode, kvalifiseringsgruppeKode);
    }

    private static boolean erIARBSMedOppfolging(Formidlingsgruppe formidlingsgruppeKode, Kvalifiseringsgruppe kvalifiseringsgruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGING_KVALIFISERINGSGRUPPEKODER.contains(kvalifiseringsgruppeKode);
    }

    public static boolean erIARBSUtenOppfolging(Formidlingsgruppe formidlingsgruppeKode, Kvalifiseringsgruppe kvalifiseringsgruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && !OPPFOLGING_KVALIFISERINGSGRUPPEKODER.contains(kvalifiseringsgruppeKode);
    }

    private static boolean erArbeidssoker(Formidlingsgruppe formidlingsgruppeKode) {
        return ARBS.equals(formidlingsgruppeKode);
    }

    public static boolean erIserv(Formidlingsgruppe formidlingsgruppe) {
        return ISERV.equals(formidlingsgruppe);
    }
}
