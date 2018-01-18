package no.nav.fo.veilarboppfolging.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.services.DigitalKontaktinformasjonService;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentReservertKrrFeilVedHentingAvDataFraKrr;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentReservertKrrHentKrrStatusSikekrhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentReservertKrrPersonIkkeFunnetIKrr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservertKrrServiceTest {

    private DigitalKontaktinformasjonService digitalKontaktinformasjonService;
    private PepClient pepClient;
    private ReservertKrrService reservertKrrService;

    @BeforeEach
    void setup() {
        digitalKontaktinformasjonService = mock(DigitalKontaktinformasjonService.class);
        pepClient = mock(PepClient.class);
        reservertKrrService = new ReservertKrrService(digitalKontaktinformasjonService, pepClient);
    }

    @Nested
    @DisplayName("Tester for operasjon hentReservertKrr")
    class HentErReservertKrrTest {

        @Test
        public void skalKalleAbac() {
            reservertKrrService.hentReservertKrr("fnr");
            verify(pepClient, times(1)).sjekkLeseTilgangTilFnr("fnr");
        }

        @Test
        public void skalKasteKorrektExceptionVedIngenTilgangFraAbac() {
            when(pepClient.sjekkLeseTilgangTilFnr(any())).thenThrow(new IngenTilgang());
            assertThrows(HentReservertKrrHentKrrStatusSikekrhetsbegrensning.class, () -> reservertKrrService.hentReservertKrr("fnr"));
        }

        @Test
        void skalKasteKorrektIngenTilgangException () {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenThrow(new NotAuthorizedException("Ingen tilgang"));
            assertThrows(HentReservertKrrHentKrrStatusSikekrhetsbegrensning.class, () -> reservertKrrService.hentReservertKrr("fnr"));
        }

        @Test
        void skalKasteKorrektIkkeFunnetException () {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenThrow(new NotFoundException());
            assertThrows(HentReservertKrrPersonIkkeFunnetIKrr.class, () -> reservertKrrService.hentReservertKrr("fnr"));
        }

        @Test
        void skalKasteKorrektGenerellException () {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenThrow(new InternalServerErrorException());
            assertThrows(HentReservertKrrFeilVedHentingAvDataFraKrr.class, () -> reservertKrrService.hentReservertKrr("fnr"));
        }

        @Test
        void skalReturnereTrue() {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenReturn(true);
            assertThat(reservertKrrService.hentReservertKrr("fnr").getReservertKrr()).isTrue();
        }

        @Test
        void skalReturnereFalse() {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenReturn(true);
            assertThat(reservertKrrService.hentReservertKrr("fnr").getReservertKrr()).isTrue();
        }
    }
}