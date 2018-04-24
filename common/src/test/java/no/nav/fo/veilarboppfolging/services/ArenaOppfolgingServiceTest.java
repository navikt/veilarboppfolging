package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Oppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusUgyldigInput;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.OppfoelgingsstatusV1;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.feil.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.feil.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.feil.UgyldigInput;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.informasjon.Formidlingsgrupper;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.informasjon.Rettighetsgrupper;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.informasjon.ServicegruppeKoder;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusResponse;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.fo.veilarboppfolging.utils.CalendarConverter.convertDateToXMLGregorianCalendar;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArenaOppfolgingServiceTest {

    private static final String MOCK_ENHET_ID = "1331";

    @InjectMocks
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    private OppfoelgingsstatusV1 oppfoelgingsstatusService;

    @Mock
    private OppfoelgingPortType oppfoelgingPortType;

    @Test
    public void hentOppfoelgingskontraktListeReturnererEnRespons() throws Exception {
        final XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(LocalDate.now().minusMonths(2));
        final XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(LocalDate.now().plusMonths(1));
        HentOppfoelgingskontraktListeResponse withOppfoelgingskontraktListe = new HentOppfoelgingskontraktListeResponse();
        Oppfoelgingskontrakt oppfoelgingskontrakt = new Oppfoelgingskontrakt();
        oppfoelgingskontrakt.setGjelderBruker(new Bruker());
        withOppfoelgingskontraktListe.getOppfoelgingskontraktListe().add(oppfoelgingskontrakt);
        when(oppfoelgingPortType.hentOppfoelgingskontraktListe(any(HentOppfoelgingskontraktListeRequest.class))).thenReturn(withOppfoelgingskontraktListe);

        final HentOppfoelgingskontraktListeResponse response = arenaOppfolgingService.hentOppfolgingskontraktListe(fom, tom, "***REMOVED***");

        assertThat(response.getOppfoelgingskontraktListe().isEmpty(), is(false));
    }

    @Test
    public void skalMappeTilOppfolgingsstatus() throws Exception {
        when(oppfoelgingsstatusService.hentOppfoelgingsstatus(any())).thenReturn(lagMockResponse());

        ArenaOppfolging arenaOppfolging = arenaOppfolgingService.hentArenaOppfolging("1234");
        Assertions.assertThat(arenaOppfolging.getFormidlingsgruppe()).isEqualTo("ARBS");
        Assertions.assertThat(arenaOppfolging.getOppfolgingsenhet()).isEqualTo(MOCK_ENHET_ID);
        Assertions.assertThat(arenaOppfolging.getRettighetsgruppe()).isEqualTo("rettighetsgruppe");
        Assertions.assertThat(arenaOppfolging.getServicegruppe()).isEqualTo("servicegruppe");
    }

    @Test(expected = NotFoundException.class)
    public void skalKasteNotFoundOmPersonIkkeFunnet() throws Exception {
        when(oppfoelgingsstatusService.hentOppfoelgingsstatus(any())).thenReturn(lagMockResponse());
        when(oppfoelgingsstatusService.hentOppfoelgingsstatus(any())).thenThrow(new HentOppfoelgingsstatusPersonIkkeFunnet("", new PersonIkkeFunnet()));
        arenaOppfolgingService.hentArenaOppfolging("1234");
    }

    @Test(expected = ForbiddenException.class)
    public void skalKasteForbiddenOmManIkkeHarTilgang() throws Exception {
        when(oppfoelgingsstatusService.hentOppfoelgingsstatus(any())).thenReturn(lagMockResponse());
        when(oppfoelgingsstatusService.hentOppfoelgingsstatus(any())).thenThrow(new HentOppfoelgingsstatusSikkerhetsbegrensning("", new Sikkerhetsbegrensning()));
        arenaOppfolgingService.hentArenaOppfolging("1234");
    }

    @Test(expected = BadRequestException.class)
    public void skalKasteBadRequestOmUgyldigIdentifikator() throws Exception {
        when(oppfoelgingsstatusService.hentOppfoelgingsstatus(any())).thenThrow(new HentOppfoelgingsstatusUgyldigInput("", new UgyldigInput()));
        arenaOppfolgingService.hentArenaOppfolging("1234");
    }

    private HentOppfoelgingsstatusResponse lagMockResponse() {
        HentOppfoelgingsstatusResponse response = new HentOppfoelgingsstatusResponse();

        Formidlingsgrupper formidlingsgrupper = new Formidlingsgrupper();
        formidlingsgrupper.setValue("ARBS");

        Rettighetsgrupper rettighetsgrupper = new Rettighetsgrupper();
        rettighetsgrupper.setValue("rettighetsgruppe");

        ServicegruppeKoder servicegruppeKoder = new ServicegruppeKoder();
        servicegruppeKoder.setValue("servicegruppe");

        response.setRettighetsgruppeKode(rettighetsgrupper);
        response.setFormidlingsgruppeKode(formidlingsgrupper);
        response.setNavOppfoelgingsenhet(MOCK_ENHET_ID);
        response.setServicegruppeKode(servicegruppeKoder);
        return response;
    }

}