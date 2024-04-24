package no.nav.veilarboppfolging.utils;

import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static no.nav.veilarboppfolging.utils.ArenaUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ArenaUtilsTest {

    private static final Set<Kvalifiseringsgruppe> KVALIFISERINGSGRUPPEKODER = new HashSet<>(
			asList(Kvalifiseringsgruppe.BATT, Kvalifiseringsgruppe.KAP11, Kvalifiseringsgruppe.IKVAL, Kvalifiseringsgruppe.IVURD, Kvalifiseringsgruppe.VURDU, Kvalifiseringsgruppe.VURDI, Kvalifiseringsgruppe.VARIG , Kvalifiseringsgruppe.OPPFI, Kvalifiseringsgruppe.BKART, Kvalifiseringsgruppe.BFORM));
	
    @Test
    public void erUnderOppfolging_default_false(){
        assertThat(erUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void erUnderOppfolging_ARBS_true() {
        alleKombinasjonerAvKvalifiseringskodeErTrue(Formidlingsgruppe.ARBS);
    }

    private void alleKombinasjonerAvKvalifiseringskodeErTrue(Formidlingsgruppe formidlingsgruppeKode) {
        assertThat(erUnderOppfolging(formidlingsgruppeKode, null)).isTrue();
        for (Kvalifiseringsgruppe kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging(formidlingsgruppeKode, kgKode)).isTrue();
        }
    }

    @Test
    public void erUnderOppfolging_ISERV_false() {
        assertThat(erUnderOppfolging(Formidlingsgruppe.ISERV, null)).isFalse();
        for (Kvalifiseringsgruppe kgKode : KVALIFISERINGSGRUPPEKODER) {
            assertThat(erUnderOppfolging(Formidlingsgruppe.ISERV, kgKode)).isFalse();
        }
    }    
    
    @Test
    public void erUnderOppfolging_IARBS_true_for_BATT_BFORM_IKVAL_VURDU_OPPFI() {
        for (Kvalifiseringsgruppe kgKode : asList( Kvalifiseringsgruppe.BATT,  Kvalifiseringsgruppe.IKVAL, Kvalifiseringsgruppe.VURDU, Kvalifiseringsgruppe.OPPFI, Kvalifiseringsgruppe.BFORM)) {
            assertThat(erUnderOppfolging(Formidlingsgruppe.IARBS, kgKode)).isTrue();
        }
    }

    @Test
    public void erUnderOppfolging_IARBS_False_for_KAP11_IVURD_VURDI_BKART() {
        assertThat(erUnderOppfolging(Formidlingsgruppe.IARBS, null)).isFalse();
        for (Kvalifiseringsgruppe kgKode : asList(Kvalifiseringsgruppe.KAP11, Kvalifiseringsgruppe.IVURD, Kvalifiseringsgruppe.VURDI, Kvalifiseringsgruppe.BKART)) {
            assertThat(erUnderOppfolging(Formidlingsgruppe.IARBS, kgKode)).isFalse();
        }
    }

    @Test
    public void erUnderOppfolning_Nar_ServiceKode_VARIG_Og_Formidlingskode_ARBS_IARBS(){
        assertThat(erUnderOppfolging(Formidlingsgruppe.ARBS, Kvalifiseringsgruppe.VARIG)).isTrue();
        assertThat(erUnderOppfolging(Formidlingsgruppe.IARBS, Kvalifiseringsgruppe.VARIG)).isTrue();
    }

    @Test
    public void kanSettesUnderOppfolging_default_false(){
        assertThat(kanSettesUnderOppfolging(null, null)).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBSogOppfolgingskoder_false(){
        OPPFOLGING_KVALIFISERINGSGRUPPEKODER.forEach((servicegruppeode) -> {
            assertThat(kanSettesUnderOppfolging( Formidlingsgruppe.IARBS, servicegruppeode)).isFalse();
        });
    }

    @Test
    public void kanSettesUnderOppfolging_IARBSogIkkeOppfolgingskoder_true(){
        asList(Kvalifiseringsgruppe.VURDI, Kvalifiseringsgruppe.BKART, Kvalifiseringsgruppe.IVURD, Kvalifiseringsgruppe.KAP11).forEach((servicegruppeode) -> {
            assertThat(kanSettesUnderOppfolging(Formidlingsgruppe.IARBS, servicegruppeode)).isTrue();
        });
    }
}