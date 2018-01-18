package no.nav.fo.veilarboppfolging.services;

import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DigitalKontaktinformasjonServiceTest {

    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;
    private DigitalKontaktinformasjonService digitalKontaktinformasjonService;

    @BeforeEach
    void setup() {
        digitalKontaktinformasjonV1 = mock(DigitalKontaktinformasjonV1.class);
        digitalKontaktinformasjonService = new DigitalKontaktinformasjonService(digitalKontaktinformasjonV1);
    }

    @Nested
    @DisplayName("Tester for operasjon erReservertKrr")
    class ReservertKrrTest {
        @Test
        void skalKasteKorrektIkkeTilgangException() throws Exception{
            when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any())).thenThrow(new HentDigitalKontaktinformasjonSikkerhetsbegrensing());
            assertThrows(NotAuthorizedException.class, () -> digitalKontaktinformasjonService.erBrukerReservertIKrr("fnr"));
        }

        @Test
        void skalKasteKorrektIkkeFunnetException() throws Exception{
            when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any())).thenThrow(new HentDigitalKontaktinformasjonPersonIkkeFunnet());
            assertThrows(NotFoundException.class, () -> digitalKontaktinformasjonService.erBrukerReservertIKrr("fnr"));
        }

        @Test
        void skalKasteKorrektIkkeFunnetException2() throws Exception{
            when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any())).thenThrow(new HentDigitalKontaktinformasjonPersonIkkeFunnet());
            assertThrows(NotFoundException.class, () -> digitalKontaktinformasjonService.erBrukerReservertIKrr("fnr"));
        }

        @Test
        public void skalReturnereTrue() throws Exception{
            when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any())).thenReturn(lagResponsMedReservasjon(true));
            assertThat(digitalKontaktinformasjonService.erBrukerReservertIKrr("fnr")).isTrue();
        }

        @Test
        public void skalReturnereFalse() throws Exception{
            when(digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(any())).thenReturn(lagResponsMedReservasjon(false));
            assertThat(digitalKontaktinformasjonService.erBrukerReservertIKrr("fnr")).isFalse();
        }

        private WSHentDigitalKontaktinformasjonResponse lagResponsMedReservasjon(boolean reservasjon) {
            WSHentDigitalKontaktinformasjonResponse response = new WSHentDigitalKontaktinformasjonResponse();
            WSKontaktinformasjon kontaktinformasjon = new WSKontaktinformasjon();
            kontaktinformasjon.setReservasjon(Boolean.toString(reservasjon));
            response.setDigitalKontaktinformasjon(kontaktinformasjon);
            return response;
        }
    }
}