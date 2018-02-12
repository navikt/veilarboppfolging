package no.nav.fo.veilarboppfolging.services.startregistrering;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.utils.FnrUtils;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.FeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.FeilVedHentingAvStatusIArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.feil.Sikkerhetsbegrensning;

import javax.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.now;
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

    public StartRegistreringStatus hentStartRegistreringStatus(String fnr) throws RegistrerBrukerSikkerhetsbegrensning,
            HentStartRegistreringStatusFeilVedHentingAvStatusFraArena, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {

        sjekkLesetilgangOrElseThrow(fnr, pepClient, (t) -> getHentStartRegistreringStatusSikkerhetsbegrensning());

        AktorId aktorId = FnrUtils.getAktorIdOrElseThrow(aktorService,fnr);

        boolean oppfolgingsflagg = arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(aktorId);

        if(oppfolgingsflagg) {
            return new StartRegistreringStatus().setUnderOppfolging(true).setOppfyllerKravForAutomatiskRegistrering(false);
        }

        Optional<ArenaOppfolging> arenaOppfolging = hentOppfolgingsstatusFraArena(fnr);

        boolean underOppfolgingIArena = arenaOppfolging.isPresent() && erUnderoppfolgingIArena(arenaOppfolging.get());

        if(underOppfolgingIArena) {
            return new StartRegistreringStatus()
                    .setUnderOppfolging(true)
                    .setOppfyllerKravForAutomatiskRegistrering(false);
        }

        boolean oppfyllerKrav = oppfyllerKravOmAutomatiskRegistrering(fnr, () -> hentArbeidsforhold(fnr), arenaOppfolging.orElse(null), LocalDate.now());

        return new StartRegistreringStatus()
                .setUnderOppfolging(false)
                .setOppfyllerKravForAutomatiskRegistrering(oppfyllerKrav);
    }

    private Optional<ArenaOppfolging> hentOppfolgingsstatusFraArena(String fnr) throws HentStartRegistreringStatusFeilVedHentingAvStatusFraArena {
        return Try.of(() -> arenaOppfolgingService.hentArenaOppfolging(fnr))
                .map(Optional::of)
                .recover(NotFoundException.class, Optional.empty())
                .onFailure((t) -> log.error("Feil ved henting av status fra Arena {}", t))
                .getOrElseThrow(t -> {
                    FeilVedHentingAvStatusIArena feilVedHentingAvStatusIArena = new FeilVedHentingAvStatusIArena();
                    feilVedHentingAvStatusIArena.setFeilkilde("Arena");
                    feilVedHentingAvStatusIArena.setFeilmelding(t.getMessage());
                    return new HentStartRegistreringStatusFeilVedHentingAvStatusFraArena("Feil ved henting av status i Arnea", feilVedHentingAvStatusIArena);
                });
    }

    @SneakyThrows
    private List<Arbeidsforhold> hentArbeidsforhold(String fnr) {
                return Try.of(() -> arbeidsforholdService.hentArbeidsforhold(fnr))
                .onFailure((t) -> log.error("Feil ved henting av arbeidsforhold", t))
                .getOrElseThrow(t -> {
                    FeilVedHentingAvArbeidsforhold feilVedHentingAvArbeidsforhold = new FeilVedHentingAvArbeidsforhold();
                    feilVedHentingAvArbeidsforhold.setFeilkilde("AAREG");
                    feilVedHentingAvArbeidsforhold.setFeilmelding(t.getMessage());
                    return new HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold("Feil ved henting av arbeidforhold", feilVedHentingAvArbeidsforhold);
                });
    }

    private RegistrerBrukerSikkerhetsbegrensning getHentStartRegistreringStatusSikkerhetsbegrensning() {
        Sikkerhetsbegrensning sikkerhetsbegrensning = getSikkerhetsbegrensning();
        return new RegistrerBrukerSikkerhetsbegrensning("Kunne ikke gi tilgang etter kall til ABAC", sikkerhetsbegrensning);
    }


    private Sikkerhetsbegrensning getSikkerhetsbegrensning() {
        Sikkerhetsbegrensning sikkerhetsbegrensning = new Sikkerhetsbegrensning();
        sikkerhetsbegrensning.setFeilaarsak("ABAC");
        sikkerhetsbegrensning.setFeilkilde("ABAC");
        sikkerhetsbegrensning.setFeilmelding("Ingen tilgang");
        sikkerhetsbegrensning.setTidspunkt(now());
        return sikkerhetsbegrensning;
    }
}
