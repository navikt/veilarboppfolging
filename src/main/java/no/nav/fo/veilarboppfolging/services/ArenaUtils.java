package no.nav.fo.veilarboppfolging.services;

import java.util.HashSet;
import java.util.Set;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;

public class ArenaUtils {

    private static final String ARBS = "ARBS";
    private static final String ISERV = "ISERV";
    private static final Set<String> DELVIS_REGISTRERT_KODER = new HashSet<>(asList("RARBS", "PARBS"));
    static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG"));
    private static final String IKKE_ARBEIDSSOKER = "IARBS";

    // Logikken som utleder om en bruker er under oppfolging kjøres også ved indeksering av brukere i VeilArbPortefølje.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode, Boolean harOppgave) {
        return erArbeidssoker(formidlingsgruppeKode, harOppgave) || erIArbeidOgHarInnsatsbehov(formidlingsgruppeKode, servicegruppeKode);
    }

    public static boolean kanSettesUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return erIARBSUtenOppfolging(formidlingsgruppeKode, servicegruppeKode);
    }

    public static boolean erIARBSUtenOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && !OPPFOLGINGKODER.contains(servicegruppeKode);
    }

    private static boolean erArbeidssoker(String formidlingsgruppeKode, Boolean harOppgave) {
        if (harOppgave == null) {
            return ARBS.equals(formidlingsgruppeKode) ||
                    erDelvisRegistrert(formidlingsgruppeKode);
        } else {
            return ARBS.equals(formidlingsgruppeKode) ||
                    erDelvisRegistrertOgHarFaattOppgaveIArena(formidlingsgruppeKode, harOppgave);
        }
    }

    private static boolean erDelvisRegistrert(String formidlingsgruppeKode) {
        return DELVIS_REGISTRERT_KODER.contains(formidlingsgruppeKode);
    }

    private static boolean erDelvisRegistrertOgHarFaattOppgaveIArena(String formidlingsgruppeKode, Boolean harOppgave) {
        return DELVIS_REGISTRERT_KODER.contains(formidlingsgruppeKode) && TRUE.equals(harOppgave);
    }

    private static boolean erIArbeidOgHarInnsatsbehov(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGINGKODER.contains(servicegruppeKode);
    }

    public static boolean erIserv(ArenaOppfolging arenaStatus) {
        return ISERV.equals(arenaStatus.getFormidlingsgruppe());
    }

    public static boolean kanReaktiveres(ArenaOppfolging arenaStatus) {
        return TRUE.equals(arenaStatus.getKanEnkeltReaktiveres());
    }

}
