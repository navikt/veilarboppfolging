package no.nav.fo.veilarboppfolging.services;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class ArenaUtils {

    private static final String ARBS = "ARBS";
    private static final Set<String> DELVIS_REGISTRERT_KODER = new HashSet<>(asList("RARBS", "PARBS"));
    private static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG"));
    private static final String IKKE_ARBEIDSSOKER = "IARBS";
    private static final String SYKEMELDT_HOS_ARBEIDSGIVER = "VURDI";

    // Logikken som utleder om en bruker er under oppfolging kjøres også ved indeksering av brukere i VeilArbPortefølje.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode, Boolean harOppgave) {
        return erArbeidssoker(formidlingsgruppeKode, harOppgave) || erIArbeidOgHarInnsatsbehov(formidlingsgruppeKode, servicegruppeKode);
    }

    public static boolean kanSettesUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode)
                && SYKEMELDT_HOS_ARBEIDSGIVER.equals(servicegruppeKode);
    }

    private static boolean erArbeidssoker(String formidlingsgruppeKode, Boolean harOppgave) {
        return ARBS.equals(formidlingsgruppeKode) 
                || DELVIS_REGISTRERT_KODER.contains(formidlingsgruppeKode) && 
                    (harOppgave == null || Boolean.TRUE.equals(harOppgave));
    }

    private static boolean erIArbeidOgHarInnsatsbehov(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGINGKODER.contains(servicegruppeKode);
    }

}
