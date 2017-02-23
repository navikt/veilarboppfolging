package no.nav.fo.veilarbsituasjon.ws;

import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.Brukervilkar;
import no.nav.fo.veilarbsituasjon.domain.Situasjon;
import no.nav.fo.veilarbsituasjon.domain.VilkarStatus;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSOppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Optional.of;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.GODKJENNT;
import static no.nav.fo.veilarbsituasjon.domain.VilkarStatus.IKKE_BESVART;
import static no.nav.fo.veilarbsituasjon.mock.OppfoelgingV1Mock.AKTIV_STATUS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetsplanSituasjonWebServiceTest {

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

    private AktivitetsplanSituasjonWebService aktivitetsplanSituasjonWebService;
    private Situasjon situasjon = new Situasjon().setAktorId(AKTOR_ID);
    private WSHentOppfoelgingskontraktListeResponse wsHentOppfoelgingskontraktListeResponse = new WSHentOppfoelgingskontraktListeResponse();
    private WSKontaktinformasjon wsKontaktinformasjon = new WSKontaktinformasjon();

    @Before
    public void setup() throws Exception {
        aktivitetsplanSituasjonWebService = new AktivitetsplanSituasjonWebService(digitalKontaktinformasjonV1, situasjonRepository, aktoerIdService, oppfoelgingPortType);
        when(oppfoelgingPortType.hentOppfoelgingskontraktListe(any(WSHentOppfoelgingskontraktListeRequest.class))).thenReturn(wsHentOppfoelgingskontraktListeResponse);
        when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class)))
                .thenReturn(new WSHentDigitalKontaktinformasjonResponse()
                        .withDigitalKontaktinformasjon(wsKontaktinformasjon));
    }

    @Test(expected = IllegalArgumentException.class)
    public void hentOppfolgingsStatus_ukjentAktor() throws Exception {
        hentOppfolgingStatus();
    }

    @Test
    public void hentOppfolgingsStatus_riktigFnr() throws Exception {
        gittAktor();
        AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();
        assertThat(oppfolgingOgVilkarStatus.fnr, equalTo(FNR));
    }

    @Test
    public void hentOppfolgingsStatus_databaseOppdateresMedRiktigSituasjon() throws Exception {
        gittAktor();
        hentOppfolgingStatus();
        verify(situasjonRepository).oppdaterSituasjon(eq(new Situasjon().setAktorId(AKTOR_ID)));
    }

    @Test
    public void hentOppfolgingsStatus_medReservasjon() throws Exception {
        gittAktor();
        gittReservasjon("reservasjon");

        AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.reservasjonKRR, is(true));
    }

    @Test
    public void hentOppfolgingsStatus_underOppfolging() throws Exception {
        gittAktor();
        gittOppfolgingStatus(AKTIV_STATUS);

        AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.underOppfolging, is(true));
    }

    @Test
    public void hentOppfolgingsStatus_aksepterVilkar() throws Exception {
        gittAktor();

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));

        besvarVilkar(GODKJENNT, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(false));
    }

    @Test
    public void hentOppfolgingsStatus_akseptererFeilVilkar() throws Exception {
        gittAktor();

        besvarVilkar(GODKJENNT, "feilVilkar");

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void hentOppfolgingsStatus_vilkarIkkeBesvart() throws Exception {
        gittAktor();

        besvarVilkar(IKKE_BESVART, hentGjeldendeVilkar());

        assertThat(hentOppfolgingStatus().vilkarMaBesvares, is(true));
    }

    @Test
    public void hentOppfolgingsStatus_underOppfolgingOgReservert() throws Exception {
        gittAktor();
        gittOppfolgingStatus(AKTIV_STATUS);
        gittReservasjon("reservasjon");

        AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();

        assertThat(oppfolgingOgVilkarStatus.manuell, is(true));
    }

    @Test
    public void hentOppfolgingsStatus_situasjonMedOppfolgingsFlaggIDatabasen() throws Exception {
        gittAktor();
        gittSituasjon(situasjon.setOppfolging(true));

        hentOppfolgingStatus();

        verifyZeroInteractions(oppfoelgingPortType);
    }

    private void besvarVilkar(VilkarStatus godkjennt, String tekst) {
        gittSituasjon(situasjon.leggTilBrukervilkar(new Brukervilkar().setTekst(tekst).setVilkarstatus(godkjennt)));
    }

    private String hentGjeldendeVilkar() throws Exception {
        return aktivitetsplanSituasjonWebService.hentVilkar();
    }

    private void gittOppfolgingStatus(String status) {
        wsHentOppfoelgingskontraktListeResponse.getOppfoelgingskontraktListe().add(new WSOppfoelgingskontrakt().withStatus(status));
    }

    private AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus hentOppfolgingStatus() throws Exception {
        return aktivitetsplanSituasjonWebService.hentOppfolgingsStatus(FNR);
    }

    private void gittSituasjon() {
        gittSituasjon(situasjon);
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