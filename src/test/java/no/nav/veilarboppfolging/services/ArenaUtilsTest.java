package no.nav.veilarboppfolging.services;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static no.nav.veilarboppfolging.utils.ArenaUtils.OPPFOLGING_SERVICEGRUPPEKODER;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erUnderOppfolging;
import static no.nav.veilarboppfolging.utils.ArenaUtils.kanSettesUnderOppfolging;
import static org.assertj.core.api.Assertions.assertThat;

public class ArenaUtilsTest {

    private static final Set<String> KVALIFISERINGSGRUPPEKODER = new HashSet<>(
			asList("BATT", "KAP11", "IKVAL", "IVURD", "VURDU", "VURDI", "VARIG", "OPPFI", "BKART", "BFORM"));
	
    @Test
    public void erUnderOppfolging_default_false(){
        assertThat(erUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void erUnderOppfolging_ARBS_true() {
        alleKombinasjonerAvKvalifiseringskodeErTrue("ARBS");
    }

    private void alleKombinasjonerAvKvalifiseringskodeErTrue(String formidlingsgruppeKode) {
        assertThat(erUnderOppfolging(formidlingsgruppeKode, null)).isTrue();
        for (String kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging(formidlingsgruppeKode, kgKode)).isTrue();
        }
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
    public void erUnderOppfolging_IARBS_False_for_KAP11_IVURD_VURDI_BKART() {
        assertThat(erUnderOppfolging("IARBS", null)).isFalse();
        for (String kgKode : asList("KAP11", "IVURD", "VURDI", "BKART")) {
            assertThat(erUnderOppfolging("IARBS", kgKode)).isFalse();
        }
    }

    @Test
    public void erUnderOppfolning_Nar_ServiceKode_VARIG_Og_Formidlingskode_ARBS_IARBS(){
        assertThat(erUnderOppfolging("ARBS","VARIG")).isTrue();
        assertThat(erUnderOppfolging("IARBS","VARIG")).isTrue();
    }

    @Test
    public void kanSettesUnderOppfolging_default_false(){
        assertThat(kanSettesUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBSogOppfolgingskoder_false(){
        OPPFOLGING_SERVICEGRUPPEKODER.forEach((servicegruppeode) -> {
            assertThat(kanSettesUnderOppfolging("IARBS", servicegruppeode)).isFalse();
        });
    }

    @Test
    public void kanSettesUnderOppfolging_IARBSogIkkeOppfolgingskoder_true(){
        asList("VURDI", "BKART", "IVURD", "KAP11").forEach((servicegruppeode) -> {
            assertThat(kanSettesUnderOppfolging("IARBS", servicegruppeode)).isTrue();
        });
    }
}