package no.nav.fo.veilarbsituasjon.services;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class ArenaUtils {

    private static final Set<String> ARBEIDSOKERKODER = new HashSet<>(asList("ARBS", "RARBS", "PARBS"));
    private static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG"));
    private static final String IKKE_ARBEIDSSOKER = "IARBS";
    private static final String SYKEMELDT_HOS_ARBEIDSGIVER = "VURDI";

    // Logikken som utleder om en bruker er under oppfolging kjøres også ved indeksering av brukere i VeilArbPortefølje.
    // Endringer i logikken må implementeres begge steder
    public static boolean erUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return erArbeidssoker(formidlingsgruppeKode) || erIArbeidOgHarInnsatsbehov(formidlingsgruppeKode, servicegruppeKode);
    }

    public static boolean kanSettesUnderOppfolging(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode)
                && SYKEMELDT_HOS_ARBEIDSGIVER.equals(servicegruppeKode);
    }

    private static boolean erArbeidssoker(String formidlingsgruppeKode) {
        return ARBEIDSOKERKODER.contains(formidlingsgruppeKode);
    }

    private static boolean erIArbeidOgHarInnsatsbehov(String formidlingsgruppeKode, String servicegruppeKode) {
        return IKKE_ARBEIDSSOKER.equals(formidlingsgruppeKode) && OPPFOLGINGKODER.contains(servicegruppeKode);
    }

}
