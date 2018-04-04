package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.NyOppfolgingTjenesteMotArenaFeature;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.mappers.ArenaOppfolgingMapper;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.*;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.OppfoelgingsstatusV1;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.informasjon.Person;
import org.slf4j.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.slf4j.LoggerFactory.getLogger;

public class ArenaOppfolgingService {

    private static final Logger LOG = getLogger(ArenaOppfolgingService.class);
    private final OppfoelgingPortType oppfoelgingPortType;
    private NyOppfolgingTjenesteMotArenaFeature nyOppfolgingTjenesteMotArenaFeature;
    private OppfoelgingsstatusV1 oppfoelgingsstatusService;

    public ArenaOppfolgingService(OppfoelgingsstatusV1 oppfoelgingsstatusService,
                                  OppfoelgingPortType oppfoelgingPortType,
                                  NyOppfolgingTjenesteMotArenaFeature nyOppfolgingTjenesteMotArenaFeature) {
        this.oppfoelgingsstatusService = oppfoelgingsstatusService;
        this.oppfoelgingPortType = oppfoelgingPortType;
        this.nyOppfolgingTjenesteMotArenaFeature = nyOppfolgingTjenesteMotArenaFeature;
    }

    public ArenaOppfolging hentArenaOppfolging(String identifikator) {
        if (nyOppfolgingTjenesteMotArenaFeature.erAktiv()) {
            return getArenaOppfolgingsstatus(identifikator);
        }
        return getArenaOppfolging(identifikator);
    }

    public HentOppfoelgingskontraktListeResponse hentOppfolgingskontraktListe(XMLGregorianCalendar fom, XMLGregorianCalendar tom, String fnr) {
        HentOppfoelgingskontraktListeRequest request = new HentOppfoelgingskontraktListeRequest();
        final Periode periode = new Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        request.setPeriode(periode);
        request.setPersonidentifikator(fnr);
        HentOppfoelgingskontraktListeResponse response;

        try {
            response = oppfoelgingPortType.hentOppfoelgingskontraktListe(request);
        } catch (HentOppfoelgingskontraktListeSikkerhetsbegrensning hentOppfoelgingskontraktListeSikkerhetsbegrensning) {
            String logMessage = "Veileder har ikke tilgang til å søke opp bruker";
            LOG.warn(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
            throw new ForbiddenException(logMessage, hentOppfoelgingskontraktListeSikkerhetsbegrensning);
        }

        return response;
    }

    private ArenaOppfolging getArenaOppfolgingsstatus(String identifikator) {
        no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusRequest request =
                new no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusRequest();

        Person person = new Person();
        person.setIdent(identifikator);
        request.setBruker(person);

        try {
            no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusResponse oppfoelgingsstatus =
                    oppfoelgingsstatusService.hentOppfoelgingsstatus(request);

            return ArenaOppfolgingMapper.mapTilArenaOppfolgingsstatus(oppfoelgingsstatus);
        } catch (no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            String logMessage = "Ikke tilgang til bruker " + identifikator;
            LOG.warn(logMessage, e);
            throw new ForbiddenException(logMessage, e);
        } catch (no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusUgyldigInput e) {
            String logMessage = "Ugyldig bruker identifikator: " + identifikator;
            LOG.warn(logMessage, e);
            throw new BadRequestException(logMessage, e);
        } catch (no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.binding.HentOppfoelgingsstatusPersonIkkeFunnet e) {
            String logMessage = "Fant ikke bruker: " + identifikator;
            LOG.warn(logMessage, e);
            throw new NotFoundException(logMessage, e);
        }
    }

    private ArenaOppfolging getArenaOppfolging(String identifikator) {
        HentOppfoelgingsstatusRequest request = new HentOppfoelgingsstatusRequest();
        request.setPersonidentifikator(identifikator);

        try {
            HentOppfoelgingsstatusResponse oppfoelgingsstatus = oppfoelgingPortType.hentOppfoelgingsstatus(request);
            return ArenaOppfolgingMapper.mapTilArenaOppfolging(oppfoelgingsstatus);
        } catch (HentOppfoelgingsstatusSikkerhetsbegrensning e) {
            String logMessage = "Ikke tilgang til bruker " + identifikator;
            LOG.warn(logMessage, e);
            throw new ForbiddenException(logMessage, e);
        } catch (HentOppfoelgingsstatusUgyldigInput e) {
            String logMessage = "Ugyldig bruker identifikator: " + identifikator;
            LOG.warn(logMessage, e);
            throw new BadRequestException(logMessage, e);
        } catch (HentOppfoelgingsstatusPersonIkkeFunnet e) {
            String logMessage = "Fant ikke bruker: " + identifikator;
            LOG.warn(logMessage, e);
            throw new NotFoundException(logMessage, e);
        }
    }
}
