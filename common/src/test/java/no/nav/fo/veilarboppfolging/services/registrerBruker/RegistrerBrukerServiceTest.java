package no.nav.fo.veilarboppfolging.services.registrerBruker;

import lombok.SneakyThrows;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.SjekkRegistrereBrukerArenaFeature;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.SjekkRegistrereBrukerGenerellFeature;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;
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
import static no.nav.fo.veilarboppfolging.utils.KonfigForRegistrertBrukerIkkeSelvgaende.NUS_KODE_2;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrerBrukerServiceTest {
    private static String FNR_OPPFYLLER_KRAV = getFodselsnummerForPersonWithAge(40);
    private static String FNR_OPPFYLLER_IKKE_KRAV = getFodselsnummerForPersonWithAge(20);

    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private AktorService aktorService;
    private PepClient pepClient;
    private ArbeidsforholdService arbeidsforholdService;
    private ArenaOppfolgingService arenaOppfolgingService;
    private RegistrerBrukerService registrerBrukerService;
    private BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private SjekkRegistrereBrukerArenaFeature erArenaToggletPaa;
    private SjekkRegistrereBrukerGenerellFeature erLagreFunksjonToggletPaa;
    private OppfolgingRepository oppfolgingRepository;
    private OppfolgingsStatusRepository statusRepository;

    @BeforeEach
    public void setup() {
        erArenaToggletPaa = mock(SjekkRegistrereBrukerArenaFeature.class);
        erLagreFunksjonToggletPaa = mock(SjekkRegistrereBrukerGenerellFeature.class);
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        pepClient = mock(PepClient.class);
        arbeidsforholdService = mock(ArbeidsforholdService.class);
        arenaOppfolgingService = mock(ArenaOppfolgingService.class);
        behandleArbeidssoekerV1 = mock(BehandleArbeidssoekerV1.class);
        oppfolgingRepository = mock(OppfolgingRepository.class);
        statusRepository = mock(OppfolgingsStatusRepository.class);

        registrerBrukerService =
                new RegistrerBrukerService(
                        arbeidssokerregistreringRepository,
                        oppfolgingRepository,
                        statusRepository,
                        pepClient,
                        aktorService,
                        arenaOppfolgingService,
                        arbeidsforholdService,
                        behandleArbeidssoekerV1,
                        erArenaToggletPaa,
                        erLagreFunksjonToggletPaa
                );

        when(aktorService.getAktorId(any())).thenReturn(Optional.of("AKTORID"));
        when(erArenaToggletPaa.erAktiv()).thenReturn(true);
        when(erLagreFunksjonToggletPaa.erAktiv()).thenReturn(true);
    }

    /*
    * Test av besvarelsene og lagring
    * */
    @Test
    void skalRegistrereSelvgaaendeBruker() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker selvgaaendeBruker = getBrukerSelvgaaende();
        RegistrertBruker registrertBruker = registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV);
        assertThat(registrertBruker).isEqualTo(selvgaaendeBruker);
    }

    @Test
    void skalRegistrereSelvgaaendeBrukerIDatabasenSelvOmArenaErToggletBort() throws Exception {
        when(erArenaToggletPaa.erAktiv()).thenReturn(false);
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker selvgaaendeBruker = getBrukerSelvgaaende();
        RegistrertBruker registrertBruker = registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV);
        verify(behandleArbeidssoekerV1, times(0)).aktiverBruker(any());
        assertThat(registrertBruker).isEqualTo(selvgaaendeBruker);
    }

    @Test
    void skalRegistrereIArenaNaarArenaToggleErPaa() throws Exception {
        when(erArenaToggletPaa.erAktiv()).thenReturn(true);
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV);
        verify(behandleArbeidssoekerV1, times(1)).aktiverBruker(any());
    }

    @Test
    void skalKasteRuntimeExceptionDersomGenerellLagreToggleErAv() throws Exception {
        when(erLagreFunksjonToggletPaa.erAktiv()).thenReturn(false);
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        assertThrows(RuntimeException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
        verify(behandleArbeidssoekerV1, times(0)).aktiverBruker(any());
    }

    @Test
    void skalKasteRuntimeExceptionDersomBrukerenSettesUnderoppfolgingUnderRegistreringsProsess() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        when(statusRepository.erOppfolgingsflaggSattForBruker(any())).thenReturn(true);
        assertThrows(RuntimeException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeRegistrereSomErUnderOppfolging() {
        mockRegistreringAvSelvgaaendeBrukerSomErUnderOppfolging();
        RegistrertBruker selvgaaendeBruker = getBrukerSelvgaaende();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeRegistrereSomIkkeOppfyllerKravForAutomatiskRegistrering() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker selvgaaendeBruker = getBrukerSelvgaaende();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_IKKE_KRAV));
    }

    @Test
    void skalIkkeRegistrereDersomIngenUtdannelse() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker ikkeSelvgaaendeBruker = getBrukerIngenUtdannelse();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeRegistrereDersomGrunnskole() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker ikkeSelvgaaendeBruker = getBrukerGrunnskole();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeRegistrereDersomUtdanningIkkeBestatt() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker ikkeSelvgaaendeBruker = getBrukerUtdanningIkkeBestatt();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeRegistrereSomHarHelseutfordringer() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker ikkeSelvgaaendeBruker = getBrukerHarHelseutfordringer();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeRegistrereDersomUtdannelseIkkeGodkjent() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker ikkeSelvgaaendeBruker = getBrukerUtdannelseIkkeGodkjent();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeRegistrereDersomSituasjonErAnnet() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        RegistrertBruker ikkeSelvgaaendeBruker = getBrukerSituasjonAnnet();
        assertThrows(RegistrerBrukerSikkerhetsbegrensning.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    /*
    * Test av kall registrering arena og lagring
    * */
    @Test
    void brukerSomIkkeFinnesIArenaSkalMappesTilNotFoundException() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        doThrow(mock(AktiverBrukerBrukerFinnesIkke.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(NotFoundException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomIkkeKanReaktiveresIArenaSkalGiServerErrorException() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        doThrow(mock(AktiverBrukerBrukerIkkeReaktivert.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomIkkeKanAktiveresIArenaSkalGiServerErrorException() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        doThrow(mock(AktiverBrukerBrukerKanIkkeAktiveres.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomManglerArbeidstillatelseSkalGiServerErrorException() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        doThrow(mock(AktiverBrukerBrukerManglerArbeidstillatelse.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(ServerErrorException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void brukerSomIkkeHarTilgangSkalGiNotAuthorizedException() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();
        doThrow(mock(AktiverBrukerSikkerhetsbegrensning.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(NotAuthorizedException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    @Test
    void ugyldigInputSkalGiBadRequestException() throws Exception {
        mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering();

        doThrow(mock(AktiverBrukerUgyldigInput.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(BadRequestException.class, () -> registrerBruker(getBrukerSelvgaaende(), FNR_OPPFYLLER_KRAV));
    }

    /*
    * Mock og hjelpe funksjoner
    * */
    private RegistrertBruker getBrukerSelvgaaende() {
        return new RegistrertBruker(
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
    private RegistrertBruker getBrukerGrunnskole() {
        return new RegistrertBruker(
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
    private RegistrertBruker getBrukerIngenUtdannelse() {
        return new RegistrertBruker(
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
    private RegistrertBruker getBrukerUtdannelseIkkeGodkjent() {
        return new RegistrertBruker(
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
    private RegistrertBruker getBrukerHarHelseutfordringer() {
        return new RegistrertBruker(
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
    private RegistrertBruker getBrukerSituasjonAnnet() {
        return new RegistrertBruker(
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
    private RegistrertBruker getBrukerUtdanningIkkeBestatt() {
        return new RegistrertBruker(
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
    private RegistrertBruker registrerBruker(RegistrertBruker bruker, String fnr) throws RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvStatusFraArena, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return registrerBrukerService.registrerBruker(bruker, fnr);
    }
    private void mockRegistreringAvSelvgaaendeBrukerSomIkkeErUnderOppfolgingOgOppfyllerKravForAutomatiskRegistrering() {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        when(arbeidssokerregistreringRepository.lagreBruker(any(), any())).thenReturn(getBrukerSelvgaaende());
    }
    private void mockRegistreringAvSelvgaaendeBrukerSomErUnderOppfolging() {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        when(arbeidssokerregistreringRepository.lagreBruker(any(), any())).thenReturn(getBrukerSelvgaaende());
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