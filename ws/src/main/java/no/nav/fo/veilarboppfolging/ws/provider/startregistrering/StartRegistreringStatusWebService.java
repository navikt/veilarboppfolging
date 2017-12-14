package no.nav.fo.veilarboppfolging.ws.provider.startregistrering;


import cxf.FeilVedHentingAvStatusFraArenaException;
import cxf.SikkerhetsbegrensningException;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.startregistreringstatus.v1.StartRegistreringStatusV1;
import no.nav.tjeneste.virksomhet.startregistreringstatus.v1.meldinger.WSStartRegistreringStatus;
import no.nav.tjeneste.virksomhet.startregistreringstatus.v1.meldinger.WSStartRegistreringStatusRequest;
import no.nav.tjeneste.virksomhet.startregistreringstatus.v1.meldinger.WSStartRegistreringStatusResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarboppfolging.utils.DateUtils.xmlGregorianCalendarToLocalDate;
import static no.nav.fo.veilarboppfolging.ws.provider.startregistrering.StartRegistreringUtils.oppfyllerKravOmAutomatiskRegistrering;


@Service
@SoapTjeneste("/StartRegistreringStatus")
@Slf4j
public class StartRegistreringStatusWebService implements StartRegistreringStatusV1 {

    private OppfoelgingPortType oppfoelgingPortType;
    private PepClient pepClient;
    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;

    public StartRegistreringStatusWebService(OppfoelgingPortType oppfoelgingPortType,
                                             PepClient pepClient,
                                             AktorService aktorService,
                                             ArbeidssokerregistreringRepository arbeidssokerregistreringRepository) {
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
    }

    public void ping() {

    }

    public WSStartRegistreringStatusResponse hentStartRegistreringStatus(WSStartRegistreringStatusRequest request)
            throws FeilVedHentingAvStatusFraArenaException, SikkerhetsbegrensningException {
        String fnr = request.getFnr();

        Try.of(() -> pepClient.sjekkLeseTilgangTilFnr(fnr))
                .onFailure(t -> log.error("Kunne ikke gi tilgang til bruker grunnet", t))
                .getOrElseThrow(t -> new SikkerhetsbegrensningException());

        AktorId aktoerId = aktorService.getAktorId(fnr)
                .map(AktorId::new)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));

        WSStartRegistreringStatusResponse response = new WSStartRegistreringStatusResponse();
        WSStartRegistreringStatus status = new WSStartRegistreringStatus();
        response.setWSStartRegistreringStatus(status);

        boolean oppfolgingsflagg = arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(aktoerId);

        if(oppfolgingsflagg) {
            status.setErUnderOppfolging(true);
            return response;
        }

        HentOppfoelgingsstatusRequest oppfoelgingsstatusRequest = new HentOppfoelgingsstatusRequest();
        oppfoelgingsstatusRequest.setPersonidentifikator(fnr);

        HentOppfoelgingsstatusResponse oppfolgingsstatusResponse =
                Try.of(() -> oppfoelgingPortType.hentOppfoelgingsstatus(oppfoelgingsstatusRequest))
                        .onFailure((t) -> log.error("Feil ved henting av status fra Arena {}", t))
                        .recover(HentOppfoelgingsstatusPersonIkkeFunnet.class, new HentOppfoelgingsstatusResponse())
                        .getOrElseThrow(FeilVedHentingAvStatusFraArenaException::new);

        boolean erUnderoppfolgingIArena = erUnderOppfolging(
                oppfolgingsstatusResponse.getFormidlingsgruppeKode(),
                oppfolgingsstatusResponse.getServicegruppeKode());

        if(erUnderoppfolgingIArena) {
            status.setErUnderOppfolging(true);
            return response;
        }

        boolean oppfyllerKrav = oppfyllerKravOmAutomatiskRegistrering(
                fnr,
                xmlGregorianCalendarToLocalDate(oppfolgingsstatusResponse.getInaktiveringsdato()),
                LocalDate.now());

        status.setErUnderOppfolging(false);
        status.setOppfyllerKrav(oppfyllerKrav);
        return response;
    }
}
