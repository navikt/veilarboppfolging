package no.nav.fo.veilarbsituasjon.services;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ArenaUtilsTest {

    @Test
    public void erUnderOppfolging_default_false(){
        assertThat(ArenaUtils.erUnderOppfolging(new WSHentOppfoelgingsstatusResponse())).isFalse();
    }

    @Test
    public void erUnderOppfolging_ARBS_true(){
        WSHentOppfoelgingsstatusResponse statusIArena = new WSHentOppfoelgingsstatusResponse();
        statusIArena.setFormidlingsgruppeKode("ARBS");
        assertThat(ArenaUtils.erUnderOppfolging(statusIArena)).isTrue();
    }

    @Test
    public void kanSettesUnderOppfolging_default_false(){
        assertThat(ArenaUtils.kanSettesUnderOppfolging(new WSHentOppfoelgingsstatusResponse())).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBS_false(){
        WSHentOppfoelgingsstatusResponse statusIArena = new WSHentOppfoelgingsstatusResponse();
        statusIArena.setFormidlingsgruppeKode("IARBS");
        assertThat(ArenaUtils.kanSettesUnderOppfolging(statusIArena)).isFalse();
    }

    @Test
    public void kanSettesUnderOppfolging_IARBSogVURDI_false(){
        WSHentOppfoelgingsstatusResponse statusIArena = new WSHentOppfoelgingsstatusResponse();
        statusIArena.setFormidlingsgruppeKode("IARBS");
        statusIArena.setServicegruppeKode("VURDI");
        assertThat(ArenaUtils.kanSettesUnderOppfolging(statusIArena)).isTrue();
    }

}