package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolging;
import no.nav.veilarboppfolging.client.veilarbarena.ArenaOppfolgingTilstand;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class ArenaUtils {

    private static final String ARBS = "ARBS";
    private static final String ISERV = "ISERV";
    private static final String IKKE_ARBEIDSSOKER = "IARBS";

    // kvalifiseringsgruppe = servicegruppe + innsatsgruppe
    public static final Set<String> OPPFOLGING_KVALIFISERINGSGRUPPEKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG"));

    // Logikken som utleder om en bruker er under oppfolging kjøres også ved indeksering av brukere i VeilArbPortefølje.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppeKode, String kvalifiseringsgruppeKode) {
        return erArbeidssoker(formidlingsgruppeKode) || erIARBSMedOppfolging(formidlingsgruppeKode, kvalifiseringsgruppeKode);
    }

    public static boolean kanSettesUnderOppfolging(String formidlingsgruppeKode, String kvalifiseringsgruppeKode) {
        return erIARBSUtenOppfolging(formidlingsgruppeKode, kvalifiseringsgruppeKode);
    }

    public static boolean kanSettesUnderOppfolging(ArenaOppfolgingTilstand arenaOppfolging, boolean erUnderOppfolging) {
        return !erUnderOppfolging && kanSettesUnderOppfolging(arenaOppfolging.getFormidlingsgruppe(), arenaOppfolging.getServicegruppe());
    }

    private static boolean erIARBSMedOppfolging(String formidlingsgruppeKode, String kvalifiseringsgruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGING_KVALIFISERINGSGRUPPEKODER.contains(kvalifiseringsgruppeKode);
    }

    public static boolean erIARBSUtenOppfolging(String formidlingsgruppeKode, String kvalifiseringsgruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && !OPPFOLGING_KVALIFISERINGSGRUPPEKODER.contains(kvalifiseringsgruppeKode);
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

    public static boolean erSykmeldtMedArbeidsgiver(ArenaOppfolgingTilstand arenaOppfolgingTilstand) {
        return ArenaUtils.erIARBSUtenOppfolging(
                arenaOppfolgingTilstand.getFormidlingsgruppe(),
                arenaOppfolgingTilstand.getServicegruppe()
        );
    }

    public static boolean erInaktivIArena(ArenaOppfolgingTilstand arenaOppfolgingTilstand) {
        return erIserv(arenaOppfolgingTilstand.getFormidlingsgruppe());
    }

}
