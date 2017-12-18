package no.nav.fo.veilarboppfolging.services;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.FeilVedHentingAvStatusIArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.GregorianCalendar;

import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarboppfolging.utils.DateUtils.xmlGregorianCalendarToLocalDate;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.oppfyllerKravOmAutomatiskRegistrering;

@Slf4j
public class StartRegistreringService {

    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private PepClient pepClient;
    private AktorService aktorService;
    private OppfoelgingPortType oppfoelgingPortType;

    public StartRegistreringService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                    PepClient pepClient,
                                    AktorService aktorService,
                                    OppfoelgingPortType oppfoelgingPortType) {
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
        this.oppfoelgingPortType = oppfoelgingPortType;
    }

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) throws HentStartRegistreringStatusSikkerhetsbegrensning,
            HentStartRegistreringStatusFeilVedHentingAvStatusFraArena {

        sjekkLesetilgang(fnr);

        AktorId aktoerId = aktorService.getAktorId(fnr)
                .map(AktorId::new)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));

        boolean oppfolgingsflagg = arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(aktoerId);

        if(oppfolgingsflagg) {
            return new StartRegistreringStatus().setUnderOppfolging(true).setOppfyllerKravForAutomatiskRegistrering(false);
        }

        HentOppfoelgingsstatusResponse oppfolgingstatusArena = hentOppfolgingsstatusFraArena(fnr);

        boolean erUnderoppfolgingIArena = erUnderOppfolging(
                oppfolgingstatusArena.getFormidlingsgruppeKode(),
                oppfolgingstatusArena.getServicegruppeKode());

        if(erUnderoppfolgingIArena) {
            return new StartRegistreringStatus()
                    .setUnderOppfolging(true)
                    .setOppfyllerKravForAutomatiskRegistrering(false);
        }

        boolean oppfyllerKrav = oppfyllerKravOmAutomatiskRegistrering(
                fnr,
                xmlGregorianCalendarToLocalDate(oppfolgingstatusArena.getInaktiveringsdato()),
                LocalDate.now());

        return new StartRegistreringStatus()
                .setUnderOppfolging(false)
                .setOppfyllerKravForAutomatiskRegistrering(oppfyllerKrav);
    }

    private void sjekkLesetilgang(String fnr) throws HentStartRegistreringStatusSikkerhetsbegrensning {
        Try.of(() -> pepClient.sjekkLeseTilgangTilFnr(fnr))
                .onFailure(t -> log.warn("Kunne ikke gi tilgang til bruker grunnet", t))
                .getOrElseThrow(t -> {
                    Sikkerhetsbegrensning sikkerhetsbegrensning = new Sikkerhetsbegrensning();
                    sikkerhetsbegrensning.setFeilaarsak("ABAC");
                    sikkerhetsbegrensning.setFeilkilde("ABAC");
                    sikkerhetsbegrensning.setFeilmelding(t.getMessage());
                    sikkerhetsbegrensning.setTidspunkt(now());
                    return new HentStartRegistreringStatusSikkerhetsbegrensning("Kunne ikke gi tilgang etter kall til ABAC", sikkerhetsbegrensning);
                });
    }

    private HentOppfoelgingsstatusResponse hentOppfolgingsstatusFraArena(String fnr) throws HentStartRegistreringStatusFeilVedHentingAvStatusFraArena {
        HentOppfoelgingsstatusRequest oppfoelgingsstatusRequest = new HentOppfoelgingsstatusRequest();
        oppfoelgingsstatusRequest.setPersonidentifikator(fnr);
        return Try.of(() -> oppfoelgingPortType.hentOppfoelgingsstatus(oppfoelgingsstatusRequest))
                .recover(HentOppfoelgingsstatusPersonIkkeFunnet.class, new HentOppfoelgingsstatusResponse())
                .onFailure((t) -> log.error("Feil ved henting av status fra Arena {}", t))
                .getOrElseThrow(t -> {
                    FeilVedHentingAvStatusIArena feilVedHentingAvStatusIArena = new FeilVedHentingAvStatusIArena();
                    feilVedHentingAvStatusIArena.setFeilkilde("Arena");
                    feilVedHentingAvStatusIArena.setFeilmelding(t.getMessage());
                    return new HentStartRegistreringStatusFeilVedHentingAvStatusFraArena("Feil ved henting av status i Arnea", feilVedHentingAvStatusIArena);
                });
    }

    @SneakyThrows
    private XMLGregorianCalendar now() {
        DatatypeFactory factory = DatatypeFactory.newInstance();
        GregorianCalendar calendar = new GregorianCalendar();
        return factory.newXMLGregorianCalendar(calendar);
    }
}
