package no.nav.fo.veilarbsituasjon.services;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ArenaUtilsTest {

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

}