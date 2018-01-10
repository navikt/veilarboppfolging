package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusRequest;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.function.Supplier;

import static no.nav.apiapp.feil.Feil.Type.UKJENT;

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
    private EnhetPepClient enhetPepClient;

    public static final Supplier<Feil> AKTOR_ID_FEIL = () -> new Feil(UKJENT, "Fant ikke aktørId for fnr");

    public void startKvp(String fnr, String begrunnelse) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String enhet = getEnhet(fnr);
        enhetPepClient.sjekkTilgang(enhet);

        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        kvpRepository.startKvp(
                aktorService.getAktorId(fnr).orElseThrow(AKTOR_ID_FEIL),
                enhet,
                veilederId,
                begrunnelse);
    }

    public void stopKvp(String fnr, String begrunnelse) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        enhetPepClient.sjekkTilgang(getEnhet(fnr));

        String veilederId = SubjectHandler.getSubjectHandler().getUid();
        kvpRepository.stopKvp(
                aktorService.getAktorId(fnr).orElseThrow(AKTOR_ID_FEIL),
                veilederId,
                begrunnelse);
    }

    @SneakyThrows
    private String getEnhet(String fnr) {
        val req = new HentOppfoelgingsstatusRequest();
        req.setPersonidentifikator(fnr);
        val res = oppfoelgingPortType.hentOppfoelgingsstatus(req);
        return res.getNavOppfoelgingsenhet();
    }

}
