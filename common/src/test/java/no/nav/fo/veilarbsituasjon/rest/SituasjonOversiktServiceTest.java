package no.nav.fo.veilarbsituasjon.rest;

import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.fo.veilarbsituasjon.vilkar.VilkarService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.*;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;

import static java.lang.System.currentTimeMillis;
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
public class SituasjonOversiktServiceTest {

    @Mock
    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1Mock;

    @Mock
    private SituasjonRepository situasjonRepositoryMock;

    @Mock
    private AktoerIdService aktoerIdServiceMcok;

    @Mock
    private VilkarService vilkarServiceMock;

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    private static final String FNR = "fnr";
    private static final String AKTOR_ID = "aktorId";

    @InjectMocks
    private SituasjonOversiktService situasjonOversiktService;

    private Situasjon situasjon = new Situasjon().setAktorId(AKTOR_ID);
    private WSHentOppfoelgingsstatusResponse hentOppfolgingstatusResponse;
    private WSKontaktinformasjon wsKontaktinformasjon = new WSKontaktinformasjon();

    @Before
    public void setup() throws Exception {
        hentOppfolgingstatusResponse = new WSHentOppfoelgingsstatusResponse();
        when(situasjonRepositoryMock.opprettSituasjon(any())).thenReturn(new Situasjon());
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any(WSHentOppfoelgingsstatusRequest.class)))
                .thenReturn(hentOppfolgingstatusResponse);
        when(digitalKontaktinformasjonV1Mock.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class)))
                .thenReturn(new WSHentDigitalKontaktinformasjonResponse()
                        .withDigitalKontaktinformasjon(wsKontaktinformasjon));
        when(vilkarServiceMock.getVilkar(null)).thenReturn("Gjeldene Vilkar");
    }

    @Test
    public void ukjentAktor() throws Exception {
        assertThrows(IllegalArgumentException.class, this::hentOppfolgingStatus);
    }

    @Test
    public void riktigFnr() throws Exception {
        gittAktor();
        gittSituasjon(situasjon);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();
        assertThat(oppfolgingStatusData.fnr, equalTo(FNR));
    }

    @Test
    public void databaseOppdateresMedRiktigSituasjon() throws Exception {
        gittAktor();
        gittSituasjon(situasjon);
        hentOppfolgingStatus();
        verify(situasjonRepositoryMock).oppdaterSituasjon(eq(new Situasjon().setAktorId(AKTOR_ID)));
    }

    @Test
    public void utenReservasjon() throws Exception {
        gittAktor();

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(false));
    }

    @Test
    public void utenKontaktInformasjon() throws Exception {
        gittAktor();
        gittKRRFeil(HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet.class);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void personIkkeFunnet() throws Exception {
        gittAktor();
        gittKRRFeil(HentDigitalKontaktinformasjonPersonIkkeFunnet.class);

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void medReservasjon() throws Exception {
        gittAktor();
        gittReservasjon("true");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.reservasjonKRR, is(true));
    }

    @Test
    public void underOppfolging() throws Exception {
        gittAktor();
        gittSituasjon(situasjon);
        gittOppfolgingStatus("ARBS", "");

        OppfolgingStatusData oppfolgingStatusData = hentOppfolgingStatus();

        assertThat(oppfolgingStatusData.underOppfolging, is(true));
    }

    @Test
    public void aksepterVilkar() throws Exception {
        gittAktor();
        gittSituasjon(situasjon);

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));

        besvarVilkar(GODKJENNT, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(false));
    }

    @Test
    public void akseptererFeilVilkar() throws Exception {
        gittAktor();
        VilkarData feilVilkar = new VilkarData().setText("feilVilkar").setHash("HASH");
        besvarVilkar(GODKJENNT, feilVilkar);

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void vilkarIkkeBesvart() throws Exception {
        gittAktor();

        besvarVilkar(IKKE_BESVART, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void ikkeArbeidssokerUnderOppfolging() throws Exception {
        gittAktor();
        gittSituasjon(situasjon);
        gittOppfolgingStatus("IARBS", "BATT");

        val oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(true));
    }

    @Test
    public void ikkeArbeidssokerIkkeUnderOppfolging() throws Exception {
        gittAktor();
        gittSituasjon(situasjon);
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

    private void besvarVilkar(VilkarStatus vilkarStatus, VilkarData vilkar) {
        gittSituasjon(situasjon.setGjeldendeBrukervilkar(
                new Brukervilkar(
                        situasjon.getAktorId(),
                        new Timestamp(currentTimeMillis()),
                        vilkarStatus,
                        vilkar.getText(),
                        vilkar.getHash()
                ))
        );
    }

    private VilkarData hentGjeldendeVilkar() throws Exception {
        return situasjonOversiktService.hentVilkar();
    }

    private void gittOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        hentOppfolgingstatusResponse.setFormidlingsgruppeKode(formidlingskode);
        hentOppfolgingstatusResponse.setServicegruppeKode(kvalifiseringsgruppekode);
    }

    private OppfolgingStatusData hentOppfolgingStatus() throws Exception {
        return situasjonOversiktService.hentOppfolgingsStatus(FNR);
    }

    private void gittSituasjon(Situasjon situasjon) {
        when(situasjonRepositoryMock.hentSituasjon(AKTOR_ID)).thenReturn(of(situasjon));
    }

    private void gittAktor() {
        when(aktoerIdServiceMcok.findAktoerId(FNR)).thenReturn(AKTOR_ID);
    }

    private void gittReservasjon(String reservasjon) {
        wsKontaktinformasjon.setReservasjon(reservasjon);
    }

    private void gittKRRFeil(Class<? extends Exception> aClass) throws Exception{
        when(digitalKontaktinformasjonV1Mock.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class))).thenThrow(aClass);
    }

}