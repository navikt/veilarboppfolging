package no.nav.fo.veilarboppfolging.services.registrerBruker;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.Sikkerhetsbegrensning;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.now;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.sjekkLesetilgangOrElseThrow;

@Slf4j
public class RegistrerBrukerService {

    private PepClient pepClient;
    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;

    public RegistrerBrukerService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository, PepClient pepClient, AktorService aktorService) {
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
    }

    public RegistrertBruker registrerBruker(RegistrertBruker bruker, String fnr) throws RegistrerBrukerSikkerhetsbegrensning {
        sjekkLesetilgangOrElseThrow(fnr, pepClient, (t) -> getHentStartRegistreringStatusSikkerhetsbegrensning());

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);

        // Todo
        // Sjekk besvarelse
        // Sjekk oppfyllerkrav og underoppfolging

        return arbeidssokerregistreringRepository.registrerBruker(bruker, aktorId);
    }

    private RegistrerBrukerSikkerhetsbegrensning getHentStartRegistreringStatusSikkerhetsbegrensning() {
        Sikkerhetsbegrensning sikkerhetsbegrensning = new Sikkerhetsbegrensning();
        sikkerhetsbegrensning.setFeilaarsak("ABAC");
        sikkerhetsbegrensning.setFeilkilde("ABAC");
        sikkerhetsbegrensning.setFeilmelding("Ingen tilgang");
        sikkerhetsbegrensning.setTidspunkt(now());
        return new RegistrerBrukerSikkerhetsbegrensning("Kunne ikke gi tilgang etter kall til ABAC", sikkerhetsbegrensning);
    }
}
