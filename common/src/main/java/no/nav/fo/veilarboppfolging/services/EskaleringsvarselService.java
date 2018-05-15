package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.BestillVarselOppgaveBrukerHarIkkeTilstrekkeligPaaloggingsnivaa;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.BestillVarselOppgaveBrukerIkkeRegistrertIIdporten;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.BestillVarselOppgaveSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.VarseloppgaveV1;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.meldinger.BestillVarselOppgaveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static no.nav.sbl.util.ExceptionUtils.throwUnchecked;
import static no.nav.sbl.util.PropertyUtils.getRequiredProperty;

@Component
public class EskaleringsvarselService {

    private static final Logger LOG = LoggerFactory.getLogger(EskaleringsvarselService.class);
    private static final String AKTIVITETSPLAN_URL_PROPERTY = "aktivitetsplan.url";
    private static final String ESKALERINGSVARSEL_OPPGAVETYPE_PROPERTY = "eskaleringsvarsel.oppgavetype";
    private static final String ESKALERINGSVARSEL_VARSELTYPE_PROPERTY = "eskaleringsvarsel.varseltype";

    @Inject
    private VarseloppgaveV1 varseloppgaveV1;

    private String aktivitetsplanBaseUrl = getRequiredProperty(AKTIVITETSPLAN_URL_PROPERTY);
    private String varseltypeId = System.getProperty(ESKALERINGSVARSEL_VARSELTYPE_PROPERTY, "DittNAV_000008");
    private String oppgavetypeId = System.getProperty(ESKALERINGSVARSEL_OPPGAVETYPE_PROPERTY, "0004");

    public void sendEskaleringsvarsel(String aktorId, long dialogId) {
        Aktoer aktor = new AktoerId().withAktoerId(aktorId);
        try {
            varseloppgaveV1.bestillVarselOppgave(lagBestillVarselOppgaveRequest(aktor, dialogId));
        } catch (BestillVarselOppgaveSikkerhetsbegrensning e) {
            LOG.error("Sikkerhetsbegrensning ved kall mot varseloppgaveV1");
            throw new IngenTilgang(e);
        } catch (BestillVarselOppgaveBrukerIkkeRegistrertIIdporten e) {
            LOG.error("Bruker ikke registert i id porten");
            throw new Feil(new BrukerIkkeRegistrertIIdporten());
        } catch (BestillVarselOppgaveBrukerHarIkkeTilstrekkeligPaaloggingsnivaa e) {
            LOG.error("Bruker har ikke tilstrekkelig innloggingsnivå");
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
        return new BestillVarselOppgaveRequest()
                .withVarselOppgaveBestilling(lagVarselOppgaveBestilling(aktoer))
                .withOppgaveHenvendelse(lagOppgaveHenvendelse(dialogId))
                .withVarselMedHandling(lagVarselMedHandling());
    }

    private VarselOppgaveBestilling lagVarselOppgaveBestilling(Aktoer aktoer) {
        String uuid = UUID.randomUUID().toString();
        return new VarselOppgaveBestilling()
                .withVarselbestillingId(uuid)
                .withMottaker(aktoer);
    }

    private OppgaveHenvendelse lagOppgaveHenvendelse(long dialogId) {
        OppgaveType oppgaveType = new OppgaveType().withValue(oppgavetypeId);
        return new OppgaveHenvendelse()
                .withOppgaveType(oppgaveType)
                .withOppgaveURL(dialogUrl(dialogId))
                .withStoppRepeterendeVarsel(true);
    }

    private VarselMedHandling lagVarselMedHandling() {
        return new VarselMedHandling().withVarseltypeId(varseltypeId);
    }
    
}
