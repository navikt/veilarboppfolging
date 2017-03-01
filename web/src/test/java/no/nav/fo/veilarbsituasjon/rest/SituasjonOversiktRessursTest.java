package no.nav.fo.veilarbsituasjon.rest;

import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.Brukervilkar;
import no.nav.fo.veilarbsituasjon.domain.Situasjon;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingOgVilkarStatus;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Optional.of;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENNT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.IKKE_BESVART;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SituasjonOversiktRessursTest {

    @Mock
    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

    @Mock
    private SituasjonRepository situasjonRepository;

    @Mock
    private AktoerIdService aktoerIdService;

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";

    @InjectMocks
    private SituasjonOversiktService aktivitetsplanSituasjonWebService;

    private Situasjon situasjon = new Situasjon().setAktorId(AKTOR_ID);
    private HentOppfoelgingsstatusResponse hentOppfolgingstatusResponse;
    private WSKontaktinformasjon wsKontaktinformasjon = new WSKontaktinformasjon();

    @Before
    public void setup() throws Exception {
        hentOppfolgingstatusResponse = new HentOppfoelgingsstatusResponse();
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any(HentOppfoelgingsstatusRequest.class)))
                .thenReturn(hentOppfolgingstatusResponse);
        when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class)))
                .thenReturn(new WSHentDigitalKontaktinformasjonResponse()
                        .withDigitalKontaktinformasjon(wsKontaktinformasjon));
    }
//
//    @Nested
//    class hentOppfolgingsStatus {

    @Test
    public void ukjentAktor() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> hentOppfolgingStatus());
    }

    @Test
    public void riktigFnr() throws Exception {
        gittAktor();
        OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();
        assertThat(oppfolgingOgVilkarStatus.fnr, equalTo(FNR));
    }

    @Test
    public void databaseOppdateresMedRiktigSituasjon() throws Exception {
        gittAktor();
        hentOppfolgingStatus();
        verify(situasjonRepository).oppdaterSituasjon(eq(new Situasjon().setAktorId(AKTOR_ID)));
    }

    @Test
    public void medReservasjon() throws Exception {
        gittAktor();
        gittReservasjon("true");

        OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.reservasjonKRR, is(true));
    }

    @Test
    public void underOppfolging() throws Exception {
        gittAktor();
        gittOppfolgingStatus("ARBS", "");

        OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(true));
    }

    @Test
    public void aksepterVilkar() throws Exception {
        gittAktor();

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));

        besvarVilkar(GODKJENNT, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(false));
    }

    @Test
    public void akseptererFeilVilkar() throws Exception {
        gittAktor();

        besvarVilkar(GODKJENNT, "feilVilkar");

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void vilkarIkkeBesvart() throws Exception {
        gittAktor();

        besvarVilkar(IKKE_BESVART, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void underOppfolgingOgReservert() throws Exception {
        gittAktor();
        gittOppfolgingStatus("ARBS", "");
        gittReservasjon("true");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.manuell, is(true));
    }

    @Test
    public void ikkeArbeidssokerUnderOppfolging() throws Exception {
        gittAktor();
        gittOppfolgingStatus("IARBS", "BATT");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(true));
    }

    @Test
    public void ikkeArbeidssokerIkkeUnderOppfolging() throws Exception {
        gittAktor();
        gittOppfolgingStatus("IARBS", "");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(false));
    }

    @Test
    public void situasjonMedOppfolgingsFlaggIDatabasen() throws Exception {
        gittAktor();
        gittSituasjon(situasjon.setOppfolging(true));

        hentOppfolgingStatus();

        verifyZeroInteractions(oppfoelgingPortType);
    }

//    }

    private void besvarVilkar(VilkarStatus godkjennt, String tekst) {
        gittSituasjon(situasjon.leggTilBrukervilkar(new Brukervilkar().setTekst(tekst).setVilkarstatus(godkjennt)));
    }

    private String hentGjeldendeVilkar() throws Exception {
        return aktivitetsplanSituasjonWebService.hentVilkar();
    }

    private void gittOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        hentOppfolgingstatusResponse.setFormidlingsgruppeKode(formidlingskode);
        hentOppfolgingstatusResponse.setServicegruppeKode(kvalifiseringsgruppekode);
    }

    private OppfolgingOgVilkarStatus hentOppfolgingStatus() throws Exception {
        return aktivitetsplanSituasjonWebService.hentOppfolgingsStatus(FNR);
    }

    private void gittSituasjon(Situasjon situasjon) {
        when(situasjonRepository.hentSituasjon(AKTOR_ID)).thenReturn(of(situasjon));
    }

    private void gittAktor() {
        when(aktoerIdService.findAktoerId(FNR)).thenReturn(AKTOR_ID);
    }

    private void gittReservasjon(String reservasjon) {
        wsKontaktinformasjon.setReservasjon(reservasjon);
    }

}