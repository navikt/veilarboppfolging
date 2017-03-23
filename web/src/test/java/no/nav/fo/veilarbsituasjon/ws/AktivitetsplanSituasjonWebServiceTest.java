package no.nav.fo.veilarbsituasjon.ws;

import lombok.val;
import no.nav.fo.veilarbsituasjon.db.SituasjonRepository;
import no.nav.fo.veilarbsituasjon.domain.*;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import no.nav.fo.veilarbsituasjon.services.PepClient;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;
import org.junit.jupiter.api.*;

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

public class AktivitetsplanSituasjonWebServiceTest {

    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1 = mock(DigitalKontaktinformasjonV1.class);
    private SituasjonRepository situasjonRepository = mock(SituasjonRepository.class);
    private AktoerIdService aktoerIdService = mock(AktoerIdService.class);
    private OppfoelgingPortType oppfoelgingPortType = mock(OppfoelgingPortType.class);
    private PepClient pepClient = mock(PepClient.class);

    private static final String FNR = "fnr";

    private static final String AKTOR_ID = "aktorId";
    private AktivitetsplanSituasjonWebService aktivitetsplanSituasjonWebService;
    private Situasjon situasjon = new Situasjon().setAktorId(AKTOR_ID);
    private WSHentOppfoelgingsstatusResponse hentOppfolgingstatusResponse;
    private WSKontaktinformasjon wsKontaktinformasjon = new WSKontaktinformasjon();

    @BeforeEach
    public void setup() throws Exception {
        aktivitetsplanSituasjonWebService = new AktivitetsplanSituasjonWebService(digitalKontaktinformasjonV1, situasjonRepository, aktoerIdService, oppfoelgingPortType, pepClient);
        hentOppfolgingstatusResponse = new WSHentOppfoelgingsstatusResponse();
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any(WSHentOppfoelgingsstatusRequest.class)))
                .thenReturn(hentOppfolgingstatusResponse);
        when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any(WSHentDigitalKontaktinformasjonRequest.class)))
                .thenReturn(new WSHentDigitalKontaktinformasjonResponse()
                        .withDigitalKontaktinformasjon(wsKontaktinformasjon));
    }

    @Nested
    class hentOppfolgingsStatus {

        @Test
        public void ukjentAktor() throws Exception {
            assertThrows(IllegalArgumentException.class, () -> hentOppfolgingStatus());
        }

        @Test
        public void riktigFnr() throws Exception {
            gittAktor();
            AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();
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

            AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();

            assertThat(oppfolgingOgVilkarStatus.reservasjonKRR, is(true));
        }

        @Test
        public void underOppfolging() throws Exception {
            gittAktor();
            gittOppfolgingStatus("ARBS", "");

            AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus oppfolgingOgVilkarStatus = hentOppfolgingStatus();

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

        // TODO Fiks test
//        @Test
//        public void underOppfolgingOgReservert() throws Exception {
//            gittAktor();
//            gittOppfolgingStatus("ARBS", "");
//            gittReservasjon("true");
//
//            val oppfolgingOgVilkarStatus = hentOppfolgingStatus();
//
//            assertThat(oppfolgingOgVilkarStatus.manuell, is(true));
//        }

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

    }

    private void besvarVilkar(VilkarStatus vilkarStatus, String tekst) {
        gittSituasjon(situasjon.setGjeldendeBrukervilkar(
                new Brukervilkar(
                        situasjon.getAktorId(),
                        new Timestamp(currentTimeMillis()),
                        vilkarStatus,
                        tekst
                ))
        );
    }

    private String hentGjeldendeVilkar() throws Exception {
        return aktivitetsplanSituasjonWebService.hentVilkar();
    }

    private void gittOppfolgingStatus(String formidlingskode, String kvalifiseringsgruppekode) {
        hentOppfolgingstatusResponse.setFormidlingsgruppeKode(formidlingskode);
        hentOppfolgingstatusResponse.setServicegruppeKode(kvalifiseringsgruppekode);
    }

    private AktivitetsplanSituasjonWebService.OppfolgingOgVilkarStatus hentOppfolgingStatus() throws Exception {
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