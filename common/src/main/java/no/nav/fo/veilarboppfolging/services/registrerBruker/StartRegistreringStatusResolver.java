package no.nav.fo.veilarboppfolging.services.registrerBruker;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.utils.ArbeidsforholdUtils;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;

import javax.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.*;

@Slf4j
public class StartRegistreringStatusResolver {

    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private PepClient pepClient;
    private ArenaOppfolgingService arenaOppfolgingService;
    private ArbeidsforholdService arbeidsforholdService;

    public StartRegistreringStatusResolver(AktorService aktorService,
                                           ArbeidssokerregistreringRepository arbeidssokerregistreringRepository,
                                           PepClient pepClient,
                                           ArenaOppfolgingService arenaOppfolgingService,
                                           ArbeidsforholdService arbeidsforholdService) {
        this.aktorService = aktorService;
        this.arbeidssokerregistreringRepository = arbeidssokerregistreringRepository;
        this.pepClient = pepClient;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.arbeidsforholdService = arbeidsforholdService;
    }

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);


        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService,fnr);

        boolean oppfolgingsflagg = arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(aktorId);

        Optional<ArenaOppfolging> arenaOppfolging = hentOppfolgingsstatusFraArena(fnr);

        boolean underOppfolgingIArena = arenaOppfolging.isPresent() && erUnderoppfolgingIArena(arenaOppfolging.get());

        if(oppfolgingsflagg && underOppfolgingIArena)  {
            return new StartRegistreringStatus().setUnderOppfolging(true).setOppfyllerKravForAutomatiskRegistrering(false);
        }

        if(underOppfolgingIArena) {
            return new StartRegistreringStatus()
                    .setUnderOppfolging(true)
                    .setOppfyllerKravForAutomatiskRegistrering(false);
        }

        boolean oppfyllerKrav = oppfyllerKravOmAutomatiskRegistrering(fnr, () -> hentAlleArbeidsforhold(fnr), arenaOppfolging.orElse(null), LocalDate.now());

        return new StartRegistreringStatus()
                .setUnderOppfolging(false)
                .setOppfyllerKravForAutomatiskRegistrering(oppfyllerKrav);
    }

    public Arbeidsforhold hentSisteArbeidsforhold(String fnr) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        return ArbeidsforholdUtils.hentSisteArbeidsforhold(hentAlleArbeidsforhold(fnr));
    }

    private Optional<ArenaOppfolging> hentOppfolgingsstatusFraArena(String fnr) {
        return Try.of(() -> arenaOppfolgingService.hentArenaOppfolging(fnr))
                .map(Optional::of)
                .recover(NotFoundException.class, Optional.empty())
                .onFailure((t) -> log.error("Feil ved henting av status fra Arena {}", t))
                .get();
    }

    @SneakyThrows
    private List<Arbeidsforhold> hentAlleArbeidsforhold(String fnr) {
                return arbeidsforholdService.hentArbeidsforhold(fnr);
    }

}
