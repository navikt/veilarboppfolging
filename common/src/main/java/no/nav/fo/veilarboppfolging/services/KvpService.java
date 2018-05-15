package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.UlovligHandling;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.domain.KodeverkBruker;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.function.Supplier;

import static no.nav.apiapp.feil.FeilType.UKJENT;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;

@Component
public class KvpService {

    @Inject
    private KvpRepository kvpRepository;

    @Inject
    private AktorService aktorService;

    @Inject
    private OppfoelgingPortType oppfoelgingPortType;

    @Inject
    private PepClient pepClient;

    @Inject
    private OppfolgingResolverDependencies oppfolgingResolverDependencies;

    private static final Supplier<Feil> AKTOR_ID_FEIL = () -> new Feil(UKJENT, "Fant ikke aktørId for fnr");

    @SneakyThrows
    public void startKvp(String fnr, String begrunnelse) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        OppfolgingResolver resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        if (!resolver.getOppfolging().isUnderOppfolging()) {
            throw new UlovligHandling();
        }

        String enhet = getEnhet(fnr);
        pepClient.sjekkTilgangTilEnhet(enhet);

        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        kvpRepository.startKvp(
                aktorService.getAktorId(fnr).orElseThrow(AKTOR_ID_FEIL),
                enhet,
                veilederId,
                begrunnelse);

        FunksjonelleMetrikker.startKvp();
    }

    @SneakyThrows
    public void stopKvp(String fnr, String begrunnelse) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        pepClient.sjekkTilgangTilEnhet(getEnhet(fnr));

        OppfolgingResolver resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        stopKvpUtenEnhetSjekk(fnr, begrunnelse, NAV, resolver);
    }

    void stopKvpUtenEnhetSjekk(String fnr, String begrunnelse, KodeverkBruker kodeverkBruker, OppfolgingResolver resolver) {
        if (resolver.harAktivEskalering()) {
            resolver.stoppEskalering("Eskalering avsluttet fordi KVP ble avsluttet");
        }

        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        kvpRepository.stopKvp(
                aktorService.getAktorId(fnr).orElseThrow(AKTOR_ID_FEIL),
                veilederId,
                begrunnelse,
                kodeverkBruker);

        FunksjonelleMetrikker.stopKvp();
    }

    Kvp gjeldendeKvp(String fnr) {
        String aktorId = aktorService.getAktorId(fnr).orElseThrow(AKTOR_ID_FEIL);
        return kvpRepository.fetch(kvpRepository.gjeldendeKvp(aktorId));
    }

    @SneakyThrows
    private String getEnhet(String fnr) {
        val req = new HentOppfoelgingsstatusRequest();
        req.setPersonidentifikator(fnr);
        val res = oppfoelgingPortType.hentOppfoelgingsstatus(req);
        return res.getNavOppfoelgingsenhet();
    }

}
