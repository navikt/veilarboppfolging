package no.nav.fo.veilarboppfolging.services.registrerBruker;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.startregistrering.StartRegistreringStatusResolver;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.Sikkerhetsbegrensning;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.now;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.sjekkLesetilgangOrElseThrow;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.erIkkeSelvgaende;

@Slf4j
public class RegistrerBrukerService {

    private PepClient pepClient;
    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private StartRegistreringStatusResolver startRegistreringStatusResolver;

    public RegistrerBrukerService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository, PepClient pepClient, AktorService aktorService, ArenaOppfolgingService arenaOppfolgingService, ArbeidsforholdService arbeidsforholdService) {
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;

        startRegistreringStatusResolver = new StartRegistreringStatusResolver(aktorService,
                arbeidssokerregistreringRepository,pepClient,arenaOppfolgingService, arbeidsforholdService);
    }

    public RegistrertBruker registrerBruker(RegistrertBruker bruker, String fnr) throws RegistrerBrukerSikkerhetsbegrensning,
            HentStartRegistreringStatusFeilVedHentingAvStatusFraArena, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        sjekkLesetilgangOrElseThrow(fnr, pepClient, (t) -> getHentStartRegistreringStatusSikkerhetsbegrensning());

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService, fnr);

        RegistrertBruker registrertBruker = null;

        StartRegistreringStatus startRegistreringStatus = startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);
        if(erIkkeSelvgaende(bruker) &&
                (!startRegistreringStatus.isUnderOppfolging() &&
                        startRegistreringStatus.isOppfyllerKravForAutomatiskRegistrering())) {
            registrertBruker = arbeidssokerregistreringRepository.registrerBruker(bruker, aktorId);
        }

        return registrertBruker;
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
