package no.nav.fo.veilarboppfolging.services;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.HentDigitalKontaktinformasjonSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.WSKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import java.util.Optional;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;

@Slf4j
public class DigitalKontaktinformasjonService {

    private DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

    public DigitalKontaktinformasjonService(DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1) {
        this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
    }

    @SuppressWarnings({"unchecked"})
    public boolean erBrukerReservertIKrr(String fnr) {
        WSHentDigitalKontaktinformasjonRequest request = new WSHentDigitalKontaktinformasjonRequest();
        request.setPersonident(fnr);
        return Try.of(() -> digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(request))
                .onFailure((t) -> log.warn("Feil ved henting av digital kontaktinformasjon for fnr {}", fnr, t))
                .map(DigitalKontaktinformasjonService::mapReservertKrrToBoolean)
                .mapFailure(
                        Case($(instanceOf(HentDigitalKontaktinformasjonSikkerhetsbegrensing.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet.class)), (t) -> new NotFoundException(t)),
                        Case($(instanceOf(HentDigitalKontaktinformasjonPersonIkkeFunnet.class)), (t) -> new NotFoundException(t))
                )
                .get();
    }

    private static boolean mapReservertKrrToBoolean(WSHentDigitalKontaktinformasjonResponse response) {
        return Optional.ofNullable(response)
                .map(WSHentDigitalKontaktinformasjonResponse::getDigitalKontaktinformasjon)
                .map(WSKontaktinformasjon::getReservasjon)
                .map("true"::equalsIgnoreCase)
                .orElseThrow(() -> {
                    String logMessage = "Respons fra DigitalKontaktinformasjon inneholder ikke status for reservasjon: {}";
                    log.error(logMessage, response);
                    return new InternalServerErrorException(logMessage);
                });
    }
}
