package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.FinnArbeidsforholdPrArbeidstakerUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerResponse;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
public class ArbeidsforholdService {

    private ArbeidsforholdV3 arbeidsforholdV3;

    public ArbeidsforholdService(ArbeidsforholdV3 arbeidsforholdV3) {
        this.arbeidsforholdV3 = arbeidsforholdV3;
    }

    public List<Arbeidsforhold> hentArbeidsforhold(String fnr) throws HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        FinnArbeidsforholdPrArbeidstakerRequest request = new FinnArbeidsforholdPrArbeidstakerRequest();
        Regelverker regelverker = new Regelverker();
        regelverker.setValue("ALLE");
        request.setRapportertSomRegelverk(regelverker);
        NorskIdent ident = new NorskIdent();
        ident.setIdent(fnr);
        request.setIdent(ident);

        FinnArbeidsforholdPrArbeidstakerResponse response;

        try {
            response = arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(request);
        } catch (FinnArbeidsforholdPrArbeidstakerSikkerhetsbegrensning e) {
            String logMessage = "Ikke tilgang til bruker " + fnr;
            log.warn(logMessage, e);
            throw new ForbiddenException(logMessage, e);
        } catch (FinnArbeidsforholdPrArbeidstakerUgyldigInput e) {
            String logMessage = "Ugyldig bruker identifikator: " + fnr;
            log.warn(logMessage, e);
            throw new BadRequestException(logMessage, e);
        }
        return response.getArbeidsforhold().stream()
                .map(Arbeidsforhold::of)
                .collect(toList());
    }
}
