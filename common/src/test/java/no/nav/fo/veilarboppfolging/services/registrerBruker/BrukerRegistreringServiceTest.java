package no.nav.fo.veilarboppfolging.services.registrerBruker;

import lombok.SneakyThrows;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.OpprettBrukerIArenaFeature;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.RegistreringFeature;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.RegistrerBrukerSikkerhetsbegrensning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerForPersonWithAge;
import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.NUS_KODE_2;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BrukerRegistreringServiceTest {
    private static String FNR_OPPFYLLER_KRAV = getFodselsnummerForPersonWithAge(40);
    private static String FNR_OPPFYLLER_IKKE_KRAV = getFodselsnummerForPersonWithAge(20);

    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private AktorService aktorService;
    private PepClient pepClient;
    private ArbeidsforholdService arbeidsforholdService;
    private ArenaOppfolgingService arenaOppfolgingService;
    private BrukerRegistreringService brukerRegistreringService;
    private BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;
    private RegistreringFeature registreringFeature;
    private OppfolgingRepository oppfolgingRepository;

    @BeforeEach
    public void setup() {
        opprettBrukerIArenaFeature = mock(OpprettBrukerIArenaFeature.class);
        registreringFeature = mock(RegistreringFeature.class);
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        pepClient = mock(PepClient.class);
        arbeidsforholdService = mock(ArbeidsforholdService.class);
        arenaOppfolgingService = mock(ArenaOppfolgingService.class);
        behandleArbeidssoekerV1 = mock(BehandleArbeidssoekerV1.class);
        oppfolgingRepository = mock(OppfolgingRepository.class);

        brukerRegistreringService =
                new BrukerRegistreringService(
                        arbeidssokerregistreringRepository,
                        oppfolgingRepository,
                        pepClient,
                        aktorService,
                        arenaOppfolgingService,
                        arbeidsforholdService,
                        behandleArbeidssoekerV1,
                        opprettBrukerIArenaFeature,
                        registreringFeature
                );

        when(aktorService.getAktorId(any())).thenReturn(Optional.of("AKTORID"));
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(true);
        when(registreringFeature.erAktiv()).thenReturn(true);
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
    }

    /*
    * Test av besvarelsene og lagring
    * */
    @Test
    void skalRegistrereSelvgaaendeBruker() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering selvgaaendeBruker = getBrukerRegistreringSelvgaaende();
        BrukerRegistrering brukerRegistrering = registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV);
        assertThat(brukerRegistrering).isEqualTo(selvgaaendeBruker);
    }

    @Test
    void skalRegistrereSelvgaaendeBrukerIDatabasenSelvOmArenaErToggletBort() throws Exception {
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(false);
        mockSelvgaaendeBruker();
        BrukerRegistrering selvgaaendeBruker = getBrukerRegistreringSelvgaaende();
        BrukerRegistrering brukerRegistrering = registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV);
        verify(behandleArbeidssoekerV1, times(0)).aktiverBruker(any());
        assertThat(brukerRegistrering).isEqualTo(selvgaaendeBruker);
    }

    @Test
    void skalRegistrereIArenaNaarArenaToggleErPaa() throws Exception {
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(true);
        mockSelvgaaendeBruker();
        registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV);
        verify(behandleArbeidssoekerV1, times(1)).aktiverBruker(any());
    }

    @Test
    void skalKasteRuntimeExceptionDersomRegistreringFeatureErAv() throws Exception {
        when(registreringFeature.erAktiv()).thenReturn(false);
        mockSelvgaaendeBruker();
        assertThrows(RuntimeException.class, () -> registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV));
        verify(behandleArbeidssoekerV1, times(0)).aktiverBruker(any());
    }

    @Test
    void skalIkkeLagreRegistreringSomErUnderOppfolging() {
        mockBrukerUnderOppfolging();
        BrukerRegistrering selvgaaendeBruker = getBrukerRegistreringSelvgaaende();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringSomIkkeOppfyllerKravForAutomatiskRegistrering() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering selvgaaendeBruker = getBrukerRegistreringSelvgaaende();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_IKKE_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringDersomIngenUtdannelse() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering ikkeSelvgaaendeBruker = getBrukerIngenUtdannelse();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistrereDersomKunGrunnskole() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringMedKunGrunnskole = getBrukerRegistreringMedKunGrunnskole();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(brukerRegistreringMedKunGrunnskole, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringDersomUtdanningIkkeBestatt() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringUtdanningIkkeBestatt = getBrukerRegistreringUtdanningIkkeBestatt();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(brukerRegistreringUtdanningIkkeBestatt, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringMedHelseutfordringer() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringMedHelseutfordringer = getBrukerRegistreringMedHelseutfordringer();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(brukerRegistreringMedHelseutfordringer, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringDersomUtdannelseIkkeGodkjent() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringUtdannelseIkkeGodkjent = getBrukerRegistreringUtdannelseIkkeGodkjent();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(brukerRegistreringUtdannelseIkkeGodkjent, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringDersomSituasjonErAnnet() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringSituasjonenAnnet = getBrukerRegistreringSituasjonenAnnet();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(brukerRegistreringSituasjonenAnnet, FNR_OPPFYLLER_KRAV));
    }

    /*
    * Test av kall registrering arena og lagring
    * */
    @Test
    void brukerSomIkkeFinnesIArenaSkalMappesTilNotFoundException() throws Exception {
        mockSelvgaaendeBruker();
        doThrow(mock(AktiverBrukerBrukerFinnesIkke.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(NotFoundException.class, () -> registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomIkkeKanReaktiveresIArenaSkalGiServerErrorException() throws Exception {
        mockSelvgaaendeBruker();
        doThrow(mock(AktiverBrukerBrukerIkkeReaktivert.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomIkkeKanAktiveresIArenaSkalGiServerErrorException() throws Exception {
        mockSelvgaaendeBruker();
        doThrow(mock(AktiverBrukerBrukerKanIkkeAktiveres.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomManglerArbeidstillatelseSkalGiServerErrorException() throws Exception {
        mockSelvgaaendeBruker();
        doThrow(mock(AktiverBrukerBrukerManglerArbeidstillatelse.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomIkkeHarTilgangSkalGiNotAuthorizedException() throws Exception {
        mockSelvgaaendeBruker();
        doThrow(mock(AktiverBrukerSikkerhetsbegrensning.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(NotAuthorizedException.class, () -> registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void ugyldigInputSkalGiBadRequestException() throws Exception {
        mockSelvgaaendeBruker();

        doThrow(mock(AktiverBrukerUgyldigInput.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(BadRequestException.class, () -> registrerBruker(getBrukerRegistreringSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    /*
    * Mock og hjelpe funksjoner
    * */
    private BrukerRegistrering getBrukerRegistreringSelvgaaende() {
        return new BrukerRegistrering(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN
        );
    }
    private BrukerRegistrering getBrukerRegistreringMedKunGrunnskole() {
        return new BrukerRegistrering(
                NUS_KODE_2,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN
        );
    }
    private BrukerRegistrering getBrukerIngenUtdannelse() {
        return new BrukerRegistrering(
                NUS_KODE_0,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN
        );
    }
    private BrukerRegistrering getBrukerRegistreringUtdannelseIkkeGodkjent() {
        return new BrukerRegistrering(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_IKKE_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN
        );
    }
    private BrukerRegistrering getBrukerRegistreringMedHelseutfordringer() {
        return new BrukerRegistrering(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_HELSEUTFORDRINGER,
                MISTET_JOBBEN
        );
    }
    private BrukerRegistrering getBrukerRegistreringSituasjonenAnnet() {
        return new BrukerRegistrering(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                SITUASJON_ANNET
        );
    }
    private BrukerRegistrering getBrukerRegistreringUtdanningIkkeBestatt() {
        return new BrukerRegistrering(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_IKKE_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN
        );
    }
    private BrukerRegistrering registrerBruker(BrukerRegistrering bruker, String fnr) throws RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvStatusFraArena, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return brukerRegistreringService.registrerBruker(bruker, fnr);
    }

    private void mockSelvgaaendeBruker() {
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        when(arbeidssokerregistreringRepository.lagreBruker(any(), any())).thenReturn(getBrukerRegistreringSelvgaaende());
    }
    private void mockBrukerUnderOppfolging() {
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        when(arbeidssokerregistreringRepository.lagreBruker(any(), any())).thenReturn(getBrukerRegistreringSelvgaaende());
    }
    private void mockArenaMedRespons(ArenaOppfolging arenaOppfolging) {
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
                .setFom(LocalDate.of(2017, 1, 10)));
    }
}