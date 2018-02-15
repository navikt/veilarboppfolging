package no.nav.fo.veilarboppfolging.services.startregistrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;

@Slf4j
public class StartRegistreringService {


    private StartRegistreringStatusResolver startRegistreringStatusResolver;
    private AktiverArbeidssokerService aktiverArbeidssokerService;

    public StartRegistreringService(ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                    PepClient pepClient,
                                    AktorService aktorService,
                                    ArenaOppfolgingService arenaOppfolgingService,
                                    ArbeidsforholdService arbeidsforholdService,
                                    AktiverArbeidssokerService aktiverArbeidssokerService) {

        this.startRegistreringStatusResolver = new StartRegistreringStatusResolver(aktorService,
                arbeidssokerregistreringRepository,pepClient,arenaOppfolgingService, arbeidsforholdService);
        this.aktiverArbeidssokerService = aktiverArbeidssokerService;
    }

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) throws HentStartRegistreringStatusFeilVedHentingAvStatusFraArena,
            RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);
    }

    public void aktiverArbeidssokerIArena(AktiverArbeidssokerData data) {
        aktiverArbeidssokerService.aktiverArbeidssoker(data);
    }
}
