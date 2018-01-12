package no.nav.fo.veilarboppfolging.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.config.DigitalKontaktinformasjonConfig;
import no.nav.fo.veilarboppfolging.services.DigitalKontaktinformasjonService;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.HentReservertKrrResponse;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentReservertKrrFeilVedHentingAvDataFraKrr;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentReservertKrrHentKrrStatusSikekrhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentReservertKrrPersonIkkeFunnetIKrr;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.FeilVedHentingAvDataFraKrr;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.PersonIkkeFunnetIKrr;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.Sikkerhetsbegrensning;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static no.nav.fo.veilarboppfolging.utils.DateUtils.now;

@Slf4j
public class ReservertKrrService {

    private DigitalKontaktinformasjonService digitalKontaktinformasjonService;
    private PepClient pepClient;

    public ReservertKrrService(DigitalKontaktinformasjonService digitalKontaktinformasjonService, PepClient pepClient) {
        this.digitalKontaktinformasjonService = digitalKontaktinformasjonService;
        this.pepClient = pepClient;
    }

    @SuppressWarnings({"unchecked"})
    public HentReservertKrrResponse hentReservertKrr(String fnr) {
        return Try.of(() -> pepClient.sjekkLeseTilgangTilFnr(fnr))
                .map(digitalKontaktinformasjonService::erBrukerReservertIKrr)
                .map(ReservertKrrService::mapToKrrResponse)
                .mapFailure(
                        Case($(instanceOf(NotAuthorizedException.class)), ReservertKrrService::ikkeTilgang),
                        Case($(instanceOf(NotFoundException.class)), ReservertKrrService::ikkeFunnet),
                        Case($(instanceOf(IngenTilgang.class)), ReservertKrrService::ikkeTilgangAbac),
                        Case($(), ReservertKrrService::generellFeil)
                )
                .get();
    }

    private static HentReservertKrrFeilVedHentingAvDataFraKrr generellFeil(Throwable t) {
        FeilVedHentingAvDataFraKrr feilmelding = new FeilVedHentingAvDataFraKrr();
        feilmelding.setFeilkilde(DigitalKontaktinformasjonConfig.URL);
        feilmelding.setFeilmelding("Feil ved henting av digital kontaktinformasjon");
        return new HentReservertKrrFeilVedHentingAvDataFraKrr("Feil ved henting av digital kontaktinformasjon",feilmelding,t);
    }

    private static HentReservertKrrPersonIkkeFunnetIKrr ikkeFunnet(Throwable t) {
        PersonIkkeFunnetIKrr feilmelding = new PersonIkkeFunnetIKrr();
        feilmelding.setFeilkilde(DigitalKontaktinformasjonConfig.URL);
        feilmelding.setFeilmelding("Kunne ikke finne digital kontaktinformasjon for person");
        return new HentReservertKrrPersonIkkeFunnetIKrr("Kunne ikke finne digital kontaktinformasjon for person", feilmelding, t);
    }

    private static HentReservertKrrHentKrrStatusSikekrhetsbegrensning ikkeTilgang(Throwable t) {
        Sikkerhetsbegrensning sikkerhetsbegrensning = getSikkerhetsbegrensning(DigitalKontaktinformasjonConfig.URL);
        return new HentReservertKrrHentKrrStatusSikekrhetsbegrensning("Ingen tilgang",sikkerhetsbegrensning,t);
    }

    private static HentReservertKrrHentKrrStatusSikekrhetsbegrensning ikkeTilgangAbac(Throwable t) {
        Sikkerhetsbegrensning sikkerhetsbegrensning = getSikkerhetsbegrensning("ABAC");
        return new HentReservertKrrHentKrrStatusSikekrhetsbegrensning("Ingen tilgang",sikkerhetsbegrensning,t);
    }

    private static Sikkerhetsbegrensning getSikkerhetsbegrensning(String feilkilde) {
        Sikkerhetsbegrensning sikkerhetsbegrensning = new Sikkerhetsbegrensning();
        sikkerhetsbegrensning.setFeilaarsak("Ingen tilgang");
        sikkerhetsbegrensning.setFeilmelding("Ingen tilgang");
        sikkerhetsbegrensning.setTidspunkt(now());
        sikkerhetsbegrensning.setFeilkilde(feilkilde);
        return sikkerhetsbegrensning;
    }

    private static HentReservertKrrResponse mapToKrrResponse(boolean reserverKrr) {
        HentReservertKrrResponse response = new HentReservertKrrResponse();
        response.setReservertKrr(reserverKrr);
        return response;
    }
}
