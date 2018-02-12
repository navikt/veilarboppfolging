package no.nav.fo.veilarboppfolging.services.startregistrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.RegistreringBruker;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;

@Slf4j
public class StartRegistreringService {


    private StartRegistreringStatusResolver startRegistreringStatusResolver;

    public StartRegistreringService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                    PepClient pepClient,
                                    AktorService aktorService,
                                    ArenaOppfolgingService arenaOppfolgingService,
                                    ArbeidsforholdService arbeidsforholdService) {
        startRegistreringStatusResolver = new StartRegistreringStatusResolver(aktorService,
                arbeidssokerregistreringRepository,pepClient,arenaOppfolgingService, arbeidsforholdService);
    }

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) throws HentStartRegistreringStatusFeilVedHentingAvStatusFraArena,
            RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);
    }

    public RegistreringBruker registrerBruker(RegistreringBruker registreringBruker, String fnr) throws RegistrerBrukerSikkerhetsbegrensning {
        return startRegistreringStatusResolver.registrerBruker(registreringBruker, fnr);
    }
}
