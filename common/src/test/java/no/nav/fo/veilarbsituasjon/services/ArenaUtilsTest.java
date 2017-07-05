package no.nav.fo.veilarbsituasjon.services;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;


public class ArenaUtilsTest {

    private static final Set<String> FORMIDLINGSGRUPPEKODER = new HashSet<>(asList("ISERV", "IARBS", "RARBS", "ARBS", "PARBS"));
	private static final Set<String> KVALIFISERINGSGRUPPEKODER = new HashSet<>(
			asList("BATT", "KAP11", "IKVAL", "IVURD", "VURDU", "VURDI", "VARIG", "OPPFI", "BKART", "BFORM"));
	
    @Test
    public void erUnderOppfolging_default_false(){
        assertThat(ArenaUtils.erUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void erUnderOppfolging_ARBS_true(){
        assertThat(ArenaUtils.erUnderOppfolging("ARBS", null)).isTrue();
    }

    @Test
    public void kanSettesUnderOppfolging_default_false(){
        assertThat(ArenaUtils.kanSettesUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBS_false(){
        assertThat(ArenaUtils.kanSettesUnderOppfolging("IARBS", null)).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBSogVURDI_false(){
        assertThat(ArenaUtils.kanSettesUnderOppfolging("IARBS", "VURDI")).isTrue();
    }

    @Test
    public void sjekkUnderOppfolging_AlleKombinasjoner() {
        for (String fgKode : FORMIDLINGSGRUPPEKODER) {
            for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
                System.out.println(String.format("[%s] [%s] - [%s]", fgKode, kgKode, ArenaUtils.erUnderOppfolging(fgKode, kgKode)));
            }
            System.out.println("-------");
        }
    }
}