package no.nav.fo.veilarbsituasjon.services;

import org.junit.Test;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarbsituasjon.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarbsituasjon.services.ArenaUtils.kanSettesUnderOppfolging;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;


public class ArenaUtilsTest {

    private static final Set<String> KVALIFISERINGSGRUPPEKODER = new HashSet<>(
			asList("BATT", "KAP11", "IKVAL", "IVURD", "VURDU", "VURDI", "VARIG", "OPPFI", "BKART", "BFORM"));
	
    @Test
    public void erUnderOppfolging_default_false(){
        assertThat(erUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void erUnderOppfolging_ARBS_true() {
        alleKombinasjonerErTrue("ARBS");
    }

    private void alleKombinasjonerErTrue(String formidlingsgruppeKode) {
        assertThat(erUnderOppfolging(formidlingsgruppeKode, null)).isTrue();
        for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging(formidlingsgruppeKode, kgKode)).isTrue();
        }
    }

    @Test
    public void erUnderOppfolging_PARBS_true() {
        alleKombinasjonerErTrue("PARBS");
    }

    @Test
    public void erUnderOppfolging_RARBS_true() {
        alleKombinasjonerErTrue("RARBS");
    }    
    
    @Test
    public void erUnderOppfolging_ISERV_false() {
        assertThat(erUnderOppfolging("ISERV", null)).isFalse();
        for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging("ISERV", kgKode)).isFalse();
        }
    }    
    
    @Test
    public void erUnderOppfolging_IARBS_true_for_BATT_BFORM_IKVAL_VURDU_OPPFI() {
        for (String kgKode : asList("BATT", "IKVAL", "VURDU", "OPPFI", "BFORM")) {
            assertThat(erUnderOppfolging("IARBS", kgKode)).isTrue();
        }
    }

    @Test
    public void erUnderOppfolging_IARBS_False_for_KAP11_IVURD_VURDI_VARIG_BKART() {
        assertThat(erUnderOppfolging("IARBS", null)).isFalse();
        for (String kgKode : asList("KAP11", "IVURD", "VURDI", "VARIG", "BKART")) {
            assertThat(erUnderOppfolging("IARBS", kgKode)).isFalse();
        }
    }

    @Test
    public void kanSettesUnderOppfolging_default_false(){
        assertThat(kanSettesUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBS_false(){
        assertThat(kanSettesUnderOppfolging("IARBS", null)).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBSogVURDI_false(){
        assertThat(kanSettesUnderOppfolging("IARBS", "VURDI")).isTrue();
    }

}