package no.nav.fo.veilarbsituasjon.services;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class ArenaUtils {

    private static final Set<String> ARBEIDSOKERKODER = new HashSet<>(asList("ARBS", "RARBS", "PARBS"));
    private static final Set<String> OPPFOLGINGKODER = new HashSet<>(asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI"));
    private static final String IKKE_ARBEIDSSOKER = "IARBS";
    private static final String SYKEMELDT_HOS_ARBEIDSGIVER = "VURDI";


    public static boolean erUnderOppfolging(WSHentOppfoelgingsstatusResponse statusIArena) {
        return erArbeidssoker(statusIArena) || erIArbeidOgHarInnsatsbehov(statusIArena);
    }

    public static boolean kanSettesUnderOppfolging(WSHentOppfoelgingsstatusResponse statusIArena) {
        return IKKE_ARBEIDSSOKER.equals(statusIArena.getFormidlingsgruppeKode())
                && SYKEMELDT_HOS_ARBEIDSGIVER.equals(statusIArena.getServicegruppeKode());
    }

    private static boolean erArbeidssoker(WSHentOppfoelgingsstatusResponse oppfolgingstatus) {
        return ARBEIDSOKERKODER.contains(oppfolgingstatus.getFormidlingsgruppeKode());
    }

    private static boolean erIArbeidOgHarInnsatsbehov(WSHentOppfoelgingsstatusResponse oppfolgingstatus) {
        return OPPFOLGINGKODER.contains(oppfolgingstatus.getServicegruppeKode());
    }

}
