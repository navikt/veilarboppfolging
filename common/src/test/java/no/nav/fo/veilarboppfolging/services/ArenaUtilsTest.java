package no.nav.fo.veilarboppfolging.services;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.kanSettesUnderOppfolging;
import static org.assertj.core.api.Assertions.assertThat;


public class ArenaUtilsTest {

    private static final Set<String> KVALIFISERINGSGRUPPEKODER = new HashSet<>(
			asList("BATT", "KAP11", "IKVAL", "IVURD", "VURDU", "VURDI", "VARIG", "OPPFI", "BKART", "BFORM"));
	
    @Test
    public void erUnderOppfolging_default_false(){
        assertThat(erUnderOppfolging(null, null, null)).isFalse();
    }

    @Test
    public void erUnderOppfolging_ARBS_true() {
        alleKombinasjonerErTrue("ARBS");
    }

    private void alleKombinasjonerErTrue(String formidlingsgruppeKode) {
        assertThat(erUnderOppfolging(formidlingsgruppeKode, null, null)).isTrue();
        for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging(formidlingsgruppeKode, kgKode, null)).isTrue();
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
        assertThat(erUnderOppfolging("ISERV", null, null)).isFalse();
        for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging("ISERV", kgKode, null)).isFalse();
        }
    }    
    
    @Test
    public void erUnderOppfolging_IARBS_true_for_BATT_BFORM_IKVAL_VURDU_OPPFI() {
        for (String kgKode : asList("BATT", "IKVAL", "VURDU", "OPPFI", "BFORM")) {
            assertThat(erUnderOppfolging("IARBS", kgKode, null)).isTrue();
        }
    }

    @Test
    public void erUnderOppfolging_IARBS_False_for_KAP11_IVURD_VURDI_BKART() {
        assertThat(erUnderOppfolging("IARBS", null, null)).isFalse();
        for (String kgKode : asList("KAP11", "IVURD", "VURDI", "BKART")) {
            assertThat(erUnderOppfolging("IARBS", kgKode, null)).isFalse();
        }
    }

    @Test
    public void erUnderOppfolning_Nar_ServiceKode_VARIG_Og_Formidlingskode_ARBS_RARBS_PARBS_IARBS(){
        assertThat(erUnderOppfolging("ARBS","VARIG", null)).isTrue();
        assertThat(erUnderOppfolging("RARBS","VARIG", null)).isTrue();
        assertThat(erUnderOppfolging("PARBS","VARIG", null)).isTrue();
        assertThat(erUnderOppfolging("IARBS","VARIG", null)).isTrue();
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