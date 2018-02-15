package no.nav.fo.veilarboppfolging.services.registrerBruker;

import lombok.SneakyThrows;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerForPersonWithAge;
import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrerBrukerServiceTest {
    private static String FNR_OPPFYLLER_KRAV = getFodselsnummerForPersonWithAge(40);


    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private AktorService aktorService;
    private PepClient pepClient;
    private ArbeidsforholdService arbeidsforholdService;
    private ArenaOppfolgingService arenaOppfolgingService;
    private RegistrerBrukerService registrerBrukerService;

    @BeforeEach
    public void setup() {
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        pepClient = mock(PepClient.class);
        arbeidsforholdService = mock(ArbeidsforholdService.class);
        arenaOppfolgingService = mock(ArenaOppfolgingService.class);
        registrerBrukerService =
                new RegistrerBrukerService(
                        arbeidssokerregistreringRepository,
                        pepClient,
                        aktorService,
                        arenaOppfolgingService,
                        arbeidsforholdService
                );

        when(aktorService.getAktorId(any())).thenReturn(Optional.of("AKTORID"));
    }
    @Test
    void skalRegistrereSelvgaaendeBruker() throws Exception {
        mockRegistreringAvSelvgaaendeBruker();

        RegistrertBruker selvgaaendeBruker = getRegistrertSelvgaaendeBruker();

        RegistrertBruker registrertBruker = registrerBruker(selvgaaendeBruker);
        assertThat(registrertBruker).isEqualTo(selvgaaendeBruker);
    }

    @Test
    void skalIkkeRegistrereIkkeSelvgaaendeBruker() throws Exception {
        mockRegistreringAvSelvgaaendeBruker();

        RegistrertBruker ikkeSelvgaaendeBruker = getRegistrertBrukerUtdanningIkkeBestatt();

        RegistrertBruker registrertBruker = registrerBruker(ikkeSelvgaaendeBruker);
        assertThat(registrertBruker).isEqualTo(null);
    }

    @Test
    void skalIkkeRegistrereBrukerUnderOppfolging() {
        // Todo
    }

    @Test
    void skalIkkeRegistrereBrukerSomIkkeOppfyllerKravForAutomatiskRegistrering() {
    }


    private RegistrertBruker getRegistrertSelvgaaendeBruker() {
        return new RegistrertBruker(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_HELSEUTFORDRINGER,
                SITUASJON
        );
    }

    private RegistrertBruker getRegistrertBrukerUtdanningIkkeBestatt() {
        return new RegistrertBruker(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_IKKE_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_HELSEUTFORDRINGER,
                SITUASJON
        );
    }

    private RegistrertBruker registrerBruker(RegistrertBruker bruker) throws RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvStatusFraArena, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return registrerBrukerService.registrerBruker(bruker, FNR_OPPFYLLER_KRAV);
    }

    private void mockRegistreringAvSelvgaaendeBruker() {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        when(arbeidssokerregistreringRepository.registrerBruker(any(), any())).thenReturn(getRegistrertSelvgaaendeBruker());
    }

    private void mockArenaMedRespons(ArenaOppfolging arenaOppfolging){
        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(arenaOppfolging);
    }

    private ArenaOppfolging arenaISERV(LocalDate iservFra) {
        return new ArenaOppfolging().setFormidlingsgruppe("ISERV").setServicegruppe("IVURD").setInaktiveringsdato(iservFra);
    }

    @SneakyThrows
    private void mockArbeidsforhold(List<Arbeidsforhold> arbeidsforhold) {
        when(arbeidsforholdService.hentArbeidsforhold(any())).thenReturn(arbeidsforhold);
    }

    private List<Arbeidsforhold> arbeidsforholdSomOppfyllerKrav() {
        return Collections.singletonList(new Arbeidsforhold()
                .setArbeidsgiverOrgnummer("orgnummer")
                .setFom(LocalDate.of(2017,1,10)));
    }
}