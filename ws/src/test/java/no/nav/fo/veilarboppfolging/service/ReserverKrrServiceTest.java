package no.nav.fo.veilarboppfolging.service;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReserverKrrServiceTest {

    private DigitalKontaktinformasjonService digitalKontaktinformasjonService;
    private ReserverKrrService reserverKrrService;

    @BeforeEach
    void setup() {
        digitalKontaktinformasjonService = mock(DigitalKontaktinformasjonService.class);
        reserverKrrService = new ReserverKrrService(digitalKontaktinformasjonService);
    }

    @Nested
    @DisplayName("Tester for operasjon hentReservertKrr")
    class HentErReservertKrrTest {
        @Test
        void skalKasteKorrektIngenTilgangException () {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenThrow(new NotAuthorizedException("Ingen tilgang"));
            assertThrows(HentReservertKrrHentKrrStatusSikekrhetsbegrensning.class, () -> reserverKrrService.hentReservertKrr("fnr"));
        }

        @Test
        void skalKasteKorrektIkkeFunnetException () {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenThrow(new NotFoundException());
            assertThrows(HentReservertKrrPersonIkkeFunnetIKrr.class, () -> reserverKrrService.hentReservertKrr("fnr"));
        }

        @Test
        void skalKasteKorrektGenerellException () {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenThrow(new InternalServerErrorException());
            assertThrows(HentReservertKrrFeilVedHentingAvDataFraKrr.class, () -> reserverKrrService.hentReservertKrr("fnr"));
        }

        @Test
        void skalReturnereTrue() {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenReturn(true);
            assertThat(reserverKrrService.hentReservertKrr("fnr").getReservertKrr()).isTrue();
        }

        @Test
        void skalReturnereFalse() {
            when(digitalKontaktinformasjonService.erBrukerReservertIKrr(any())).thenReturn(true);
            assertThat(reserverKrrService.hentReservertKrr("fnr").getReservertKrr()).isTrue();
        }
    }
}