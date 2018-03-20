package no.nav.fo.veilarboppfolging.services.registrerBruker;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.OpprettBrukerIArenaFeature;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig.RegistreringFeature;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
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
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerForPersonWithAge;
import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.NUS_KODE_2;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BrukerRegistreringServiceTest {
    private static String FNR_OPPFYLLER_KRAV = getFodselsnummerForPersonWithAge(40);
    private static String FNR_OPPFYLLER_IKKE_KRAV = getFodselsnummerForPersonWithAge(20);

    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private AktorService aktorService;
    private BrukerRegistreringService brukerRegistreringService;
    private BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;
    private RegistreringFeature registreringFeature;
    private OppfolgingRepository oppfolgingRepository;
    private NyeBrukereFeedRepository nyeBrukereFeedRepository;
    private StartRegistreringStatusResolver startRegistreringStatusResolver;

    @BeforeEach
    public void setup() {
        opprettBrukerIArenaFeature = mock(OpprettBrukerIArenaFeature.class);
        registreringFeature = mock(RegistreringFeature.class);
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        behandleArbeidssoekerV1 = mock(BehandleArbeidssoekerV1.class);
        oppfolgingRepository = mock(OppfolgingRepository.class);
        nyeBrukereFeedRepository = mock(NyeBrukereFeedRepository.class);
        startRegistreringStatusResolver = mock(StartRegistreringStatusResolver.class);


        brukerRegistreringService =
                new BrukerRegistreringService(
                        arbeidssokerregistreringRepository,
                        oppfolgingRepository,
                        aktorService,
                        behandleArbeidssoekerV1,
                        opprettBrukerIArenaFeature,
                        registreringFeature,
                        nyeBrukereFeedRepository,
                        startRegistreringStatusResolver
                );

        when(aktorService.getAktorId(any())).thenReturn(Optional.of("AKTORID"));
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(true);
        when(registreringFeature.erAktiv()).thenReturn(true);
    }

    /*
    * Test av besvarelsene og lagring
    * */
    @Test
    void skalRegistrereSelvgaaendeBruker() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering selvgaaendeBruker = getBrukerRegistreringSelvgaaende();
        registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV);
        verify(arbeidssokerregistreringRepository, times(1)).lagreBruker(any(), any());
    }

    @Test
    void skalRegistrereSelvgaaendeBrukerIDatabasenSelvOmArenaErToggletBort() throws Exception {
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(false);
        mockSelvgaaendeBruker();
        BrukerRegistrering selvgaaendeBruker = getBrukerRegistreringSelvgaaende();
        registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV);
        verify(behandleArbeidssoekerV1, times(0)).aktiverBruker(any());
        verify(arbeidssokerregistreringRepository, times(1)).lagreBruker(any(), any());
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
        assertThrows(RuntimeException.class, () -> registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringSomIkkeOppfyllerKravForAutomatiskRegistrering() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering selvgaaendeBruker = getBrukerIngenUtdannelse();
        assertThrows(RuntimeException.class, () -> registrerBruker(selvgaaendeBruker, FNR_OPPFYLLER_IKKE_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringDersomIngenUtdannelse() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering ikkeSelvgaaendeBruker = getBrukerIngenUtdannelse();
        assertThrows(RuntimeException.class, () -> registrerBruker(ikkeSelvgaaendeBruker, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistrereDersomKunGrunnskole() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringMedKunGrunnskole = getBrukerRegistreringMedKunGrunnskole();
        assertThrows(RuntimeException.class, () -> registrerBruker(brukerRegistreringMedKunGrunnskole, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringDersomUtdanningIkkeBestatt() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringUtdanningIkkeBestatt = getBrukerRegistreringUtdanningIkkeBestatt();
        assertThrows(RuntimeException.class, () -> registrerBruker(brukerRegistreringUtdanningIkkeBestatt, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringMedHelseutfordringer() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringMedHelseutfordringer = getBrukerRegistreringMedHelseutfordringer();
        assertThrows(RuntimeException.class, () -> registrerBruker(brukerRegistreringMedHelseutfordringer, FNR_OPPFYLLER_KRAV));
    }

    @Test
    void skalIkkeLagreRegistreringDersomUtdannelseIkkeGodkjent() throws Exception {
        mockSelvgaaendeBruker();
        BrukerRegistrering brukerRegistreringUtdannelseIkkeGodkjent = getBrukerRegistreringUtdannelseIkkeGodkjent();
        assertThrows(RuntimeException.class, () -> registrerBruker(brukerRegistreringUtdannelseIkkeGodkjent, FNR_OPPFYLLER_KRAV));
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
    static BrukerRegistrering getBrukerRegistreringSelvgaaende() {
        return BrukerRegistrering.builder()
                .nusKode(NUS_KODE_4)
                .yrkesPraksis("1111.11")
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
    }
    private BrukerRegistrering getBrukerRegistreringMedKunGrunnskole() {
        return BrukerRegistrering.builder()
                .nusKode(NUS_KODE_2)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
    }
    private BrukerRegistrering getBrukerIngenUtdannelse() {
        return BrukerRegistrering.builder()
                .nusKode(NUS_KODE_0)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
    }
    private BrukerRegistrering getBrukerRegistreringUtdannelseIkkeGodkjent() {
        return BrukerRegistrering.builder()
                .nusKode(NUS_KODE_4)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_IKKE_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
    }
    private BrukerRegistrering getBrukerRegistreringMedHelseutfordringer() {
        return BrukerRegistrering.builder()
                .nusKode(NUS_KODE_4)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_HELSEUTFORDRINGER)
                .build();
    }

    private BrukerRegistrering getBrukerRegistreringUtdanningIkkeBestatt() {
        return BrukerRegistrering.builder()
                .nusKode(NUS_KODE_4)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_IKKE_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
    }
    private BrukerRegistrering registrerBruker(BrukerRegistrering bruker, String fnr) throws RegistrerBrukerSikkerhetsbegrensning, HentStartRegistreringStatusFeilVedHentingAvStatusFraArena, HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold {
        return brukerRegistreringService.registrerBruker(bruker, fnr);
    }

    private void mockBrukerUnderOppfolging() {
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        when(arbeidssokerregistreringRepository.lagreBruker(any(), any())).thenReturn(getBrukerRegistreringSelvgaaende());
    }

    private void mockSelvgaaendeBruker() {
        when(startRegistreringStatusResolver.hentStartRegistreringStatus(any())).thenReturn(
                new StartRegistreringStatus()
                        .setUnderOppfolging(false)
                        .setOppfyllerKravForAutomatiskRegistrering(true)
        );
    }

}