package no.nav.veilarboppfolging.services;

import java.util.HashSet;
import java.util.Set;

import no.nav.veilarboppfolging.domain.ArenaOppfolging;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;

public class ArenaUtils {

    private static final String ARBS = "ARBS";
    private static final String ISERV = "ISERV";
    static final Set<String> OPPFOLGING_SERVICEGRUPPEKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG"));
    private static final String IKKE_ARBEIDSSOKER = "IARBS";

    // Logikken som utleder om en bruker er under oppfolging kjøres også ved indeksering av brukere i VeilArbPortefølje.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return erArbeidssoker(formidlingsgruppeKode) || erIARBSMedOppfolging(formidlingsgruppeKode, servicegruppeKode);
    }

    public static boolean kanSettesUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return erIARBSUtenOppfolging(formidlingsgruppeKode, servicegruppeKode);
    }

    private static boolean erIARBSMedOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGING_SERVICEGRUPPEKODER.contains(servicegruppeKode);
    }

    public static boolean erIARBSUtenOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && !OPPFOLGING_SERVICEGRUPPEKODER.contains(servicegruppeKode);
    }

    private static boolean erArbeidssoker(String formidlingsgruppeKode) {
        return ARBS.equals(formidlingsgruppeKode);
    }

    public static boolean erIserv(ArenaOppfolging arenaOppfolging) {
        return erIserv(arenaOppfolging.getFormidlingsgruppe());
    }

    public static boolean erIserv(String formidlingsgruppe) {
        return ISERV.equals(formidlingsgruppe);
    }

    public static boolean kanEnkeltReaktiveres(ArenaOppfolging arenaStatus) {
        return TRUE.equals(arenaStatus.getKanEnkeltReaktiveres());
    }

}
