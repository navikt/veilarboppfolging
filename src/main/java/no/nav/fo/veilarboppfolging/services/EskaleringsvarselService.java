package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveBrukerHarIkkeTilstrekkeligPaaloggingsnivaa;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveBrukerIkkeRegistrertIIdporten;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.VarseloppgaveV1;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.meldinger.BestillVarselOppgaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.AKTIVITETSPLAN_URL_PROPERTY;
import static no.nav.sbl.util.ExceptionUtils.throwUnchecked;
import static no.nav.sbl.util.PropertyUtils.getRequiredProperty;

@Component
public class EskaleringsvarselService {

    private static final Logger LOG = LoggerFactory.getLogger(EskaleringsvarselService.class);

    @Inject
    private VarseloppgaveV1 varseloppgaveV1;

    private String aktivitetsplanBaseUrl = getRequiredProperty(AKTIVITETSPLAN_URL_PROPERTY);

    public void sendEskaleringsvarsel(String aktorId, long dialogId) {
        AktoerId aktor = new AktoerId();
        aktor.setAktoerId(aktorId);
        try {
            varseloppgaveV1.bestillVarselOppgave(lagBestillVarselOppgaveRequest(aktor, dialogId));
        } catch (BestillVarselOppgaveSikkerhetsbegrensning e) {
            LOG.error("Sikkerhetsbegrensning ved kall mot varseloppgaveV1 aktørId {} ", aktorId);
            throw new IngenTilgang(e);
        } catch (BestillVarselOppgaveBrukerIkkeRegistrertIIdporten e) {
            LOG.error("Bruker aktørId {}  ikke registert i id porten", aktor);
            throw new Feil(new BrukerIkkeRegistrertIIdporten());
        } catch (BestillVarselOppgaveBrukerHarIkkeTilstrekkeligPaaloggingsnivaa e) {
            LOG.error("Bruker aktørId {} har ikke tilstrekkelig innloggingsnivå", aktor);
            throw new Feil(new BrukerHarIkkeTilstrekkeligPaaloggingsnivaa());
        } catch (Exception e) {
            LOG.error("Sending av eskaleringsvarsel feilet for aktørId {} og dialogId {}", aktorId, dialogId, e);
            throw throwUnchecked(e);
        }
    }

    public class BrukerIkkeRegistrertIIdporten implements Feil.Type {
        @Override
        public String getName() {
            return "BRUKER_IKKE_REGISTRERT_I_IDPORTEN";
        }

        @Override
        public Response.Status getStatus() {
            return Response.Status.BAD_REQUEST;
        }
    }

    public class BrukerHarIkkeTilstrekkeligPaaloggingsnivaa implements Feil.Type {
        @Override
        public String getName() {
            return "BRUKER_HAR_IKKE_TILSTREKKELIG_PAALOGGINGSNIVAA";
        }

        @Override
        public Response.Status getStatus() {
            return Response.Status.BAD_REQUEST;
        }
    }


    protected String dialogUrl(long dialogId) {
        return aktivitetsplanBaseUrl + "/dialog/" + dialogId;
    }

    private BestillVarselOppgaveRequest lagBestillVarselOppgaveRequest(Aktoer aktoer, long dialogId) {
        BestillVarselOppgaveRequest bestillVarselOppgaveRequest = new BestillVarselOppgaveRequest();
        bestillVarselOppgaveRequest.setVarselOppgaveBestilling(lagVarselOppgaveBestilling(aktoer));
        bestillVarselOppgaveRequest.setOppgaveHenvendelse(lagOppgaveHenvendelse(dialogId));
        bestillVarselOppgaveRequest.setVarselMedHandling(lagVarselMedHandling());

        return bestillVarselOppgaveRequest;
    }

    private VarselOppgaveBestilling lagVarselOppgaveBestilling(Aktoer aktoer) {
        String uuid = UUID.randomUUID().toString();
        VarselOppgaveBestilling varselOppgaveBestilling = new VarselOppgaveBestilling();
        varselOppgaveBestilling.setVarselbestillingId(uuid);
        varselOppgaveBestilling.setMottaker(aktoer);

        return varselOppgaveBestilling;

    }

    private OppgaveHenvendelse lagOppgaveHenvendelse(long dialogId) {
        OppgaveType oppgaveType = new OppgaveType();
        oppgaveType.setValue("0004");

        OppgaveHenvendelse oppgaveHenvendelse = new OppgaveHenvendelse();
        oppgaveHenvendelse.setOppgaveType(oppgaveType);
        oppgaveHenvendelse.setOppgaveURL(dialogUrl(dialogId));
        oppgaveHenvendelse.setStoppRepeterendeVarsel(true);

        return oppgaveHenvendelse;
    }

    private VarselMedHandling lagVarselMedHandling() {
        VarselMedHandling varselMedHandling = new VarselMedHandling();
        varselMedHandling.setVarseltypeId("DittNAV_000008");

        return varselMedHandling;
    }

}
