package no.nav.fo.veilarbsituasjon.ws.provider;


import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.soap.SoapTjeneste;
import no.nav.fo.veilarbsituasjon.domain.AktorId;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingStatusData;
import no.nav.fo.veilarbsituasjon.services.SituasjonOversiktService;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.OppfolgingsinfoV1;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.feil.WSSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusResponse;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.WSOppfolgingsdata;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;

import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;

import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;

@Service
@SoapTjeneste("/oppfolgingsinfo")
public class OppfolgingsinfoWebService implements OppfolgingsinfoV1 {

    @Inject
    private SituasjonOversiktService situasjonOversiktService;

    @Override
    public OppfolgingsstatusResponse hentOppfolgingsstatus(OppfolgingsstatusRequest request) {
        OppfolgingStatusData oppfolgingStatusData = null;
        OppfolgingsstatusResponse response = new OppfolgingsstatusResponse();
        try {
            oppfolgingStatusData = situasjonOversiktService.hentOppfolgingsStatus(new AktorId(request.getAktorId()));
            response.setWsOppfolgingsdata(wsOppfolgingsdataof(oppfolgingStatusData, request.getAktorId()));
            return response;
        } catch (IngenTilgang ingenTilgang) {
            return new OppfolgingsstatusResponse()
                    .withWsSikkerhetsbegrensning(new WSSikkerhetsbegrensning()
                            .withFeilkilde("ABAC")
                            .withFeilmelding("Ingen tilgang til bruker"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void ping() {

    }

    private static WSOppfolgingsdata wsOppfolgingsdataof(OppfolgingStatusData statusData, String aktoerid) {
        WSOppfolgingsdata oppfolgingsdata = new WSOppfolgingsdata();
        oppfolgingsdata.setAktorId(aktoerid);
        oppfolgingsdata.setErUnderOppfolging(statusData.isUnderOppfolging());
        oppfolgingsdata.setManuell(statusData.isManuell());
        oppfolgingsdata.setReservasjonKRR(statusData.isReservasjonKRR());
        oppfolgingsdata.setVeilederIdent(statusData.getVeilederId());
        oppfolgingsdata.setOppfolgingUtgang(kanskjeCalendar(statusData.getOppfolgingUtgang()));
        oppfolgingsdata.setVilkarMaBesvares(statusData.isVilkarMaBesvares());
        return oppfolgingsdata;
    }

    private static XMLGregorianCalendar kanskjeCalendar(Date date) {
        if(Objects.isNull(date)) {
            return null;
        }
        return convertDateToXMLGregorianCalendar(LocalDate.ofEpochDay(date.toInstant().getEpochSecond()));
    }
}
