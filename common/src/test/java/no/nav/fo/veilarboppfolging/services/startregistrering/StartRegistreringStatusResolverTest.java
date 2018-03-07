package no.nav.fo.veilarboppfolging.services.startregistrering;

import lombok.SneakyThrows;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.registrerBruker.StartRegistreringStatusResolver;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerForPersonWithAge;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StartRegistreringStatusResolverTest {
    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private PepClient pepClient;
    private ArbeidsforholdService arbeidsforholdService;

    private ArenaOppfolgingService arenaOppfolgingService;
    private StartRegistreringStatusResolver startRegistreringStatusResolver;

    private static String FNR_OPPFYLLER_KRAV = getFodselsnummerForPersonWithAge(40);
    private static String FNR_OPPFYLLER_IKKE_KRAV = getFodselsnummerForPersonWithAge(20);


    @BeforeEach
    public void setup() {
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        pepClient = mock(PepClient.class);
        arbeidsforholdService = mock(ArbeidsforholdService.class);
        arenaOppfolgingService = mock(ArenaOppfolgingService.class);
        startRegistreringStatusResolver =
                new StartRegistreringStatusResolver(
                        aktorService,
                        arbeidssokerregistreringRepository,
                        pepClient,
                        arenaOppfolgingService,
                        arbeidsforholdService
                );

        when(aktorService.getAktorId(any())).thenReturn(Optional.of("AKTORID"));
    }

    @Test
    public void skalSjekkeTilgangTilBruker() {
        mockFinnesIkkeIArena();
        getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        verify(pepClient, times(1)).sjekkLeseTilgangTilFnr(any());
    }

    @Test
    public void skalIkkeVareUnderoppfolgingNaarBrukerIkkeFinnesIArena() {
        mockFinnesIkkeIArena();
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());

        StartRegistreringStatus startRegistreringStatus = getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        assertThat(startRegistreringStatus.isUnderOppfolging()).isFalse();
    }

    @Test
    public void skalHenteStatusFraArenaDersomOppfolgingsflaggIkkeErSatt() {
        mockFinnesIkkeIArena();
        getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        verify(arenaOppfolgingService, timeout(1)).hentArenaOppfolging(any());
    }

    @Test
    public void skalIkkeKalleArenaDersomOppfolgignsflaggErSatt() {
        mockOppfolgingsflagg();
        getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        verify(arenaOppfolgingService, never()).hentArenaOppfolging(any());
    }

    @Test
    public void skalIkkeHenteArbeidsforholdOmBrukerErUnderOppfolging() throws Exception {
        mockOppfolgingsflagg();
        getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        verify(arbeidsforholdService, never()).hentArbeidsforhold(any());
    }

    @Test
    public void skalReturnerUnderOppfolgingNaarUnderOppfolgingIArena() {
        mockArenaMedRespons(underOppfolgingIArena());
        StartRegistreringStatus startRegistreringStatus = getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        assertThat(startRegistreringStatus.isUnderOppfolging()).isTrue();
    }

    @Test
    public void skalReturnereFalseOmIkkeUnderOppfolgingIArena() {
        mockArenaMedRespons(arenaISERV(LocalDate.now()));
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        StartRegistreringStatus startRegistreringStatus = getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        assertThat(startRegistreringStatus.isUnderOppfolging()).isFalse();
    }

    @Test
    public void skalReturnereTrueDersomBrukerOppfyllerKrav() {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        StartRegistreringStatus startRegistreringStatus = getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        assertThat(startRegistreringStatus.isOppfyllerKravForAutomatiskRegistrering()).isTrue();
    }

    @Test
    public void skalIkkeOppfylleKravPgaAlder() {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        StartRegistreringStatus startRegistreringStatus = getStartRegistreringStatus(FNR_OPPFYLLER_IKKE_KRAV);
        assertThat(startRegistreringStatus.isOppfyllerKravForAutomatiskRegistrering()).isFalse();
    }

    @Test
    public void skalIkkeOppfylleKravPgaIservDato() {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(1)));
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        StartRegistreringStatus startRegistreringStatus = getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        assertThat(startRegistreringStatus.isOppfyllerKravForAutomatiskRegistrering()).isFalse();
    }

    @Test
    public void skalIkkeOppfylleKravPgaArbeidserfaring() {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        mockArbeidsforhold(Collections.emptyList());
        StartRegistreringStatus startRegistreringStatus = getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        assertThat(startRegistreringStatus.isOppfyllerKravForAutomatiskRegistrering()).isFalse();
    }

    @Test
    public void skalIkkeHenteArbeidsforholdDersomBrukerIkkeOppfyllerKravOmAlder() throws Exception {
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        getStartRegistreringStatus(FNR_OPPFYLLER_IKKE_KRAV);
        verify(arbeidsforholdService, never()).hentArbeidsforhold(any());
    }

    @Test
    public void skalKasterKorrektExceptionDersomKallTilArenaFeiler() throws Exception {
        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenThrow(Exception.class);
        assertThrows(HentStartRegistreringStatusFeilVedHentingAvStatusFraArena.class, () -> getStartRegistreringStatus(FNR_OPPFYLLER_KRAV));
    }

    @Test
    public void skalKasterKorrektExceptionDersomKallTilArbeidsforholdFeiler() throws Exception{
        mockArenaMedRespons(arenaISERV(LocalDate.now().minusYears(2)));
        when(arbeidsforholdService.hentArbeidsforhold(any())).thenThrow(Exception.class);
        assertThrows(HentStartRegistreringStatusFeilVedHentingAvArbeidsforhold.class, () -> getStartRegistreringStatus(FNR_OPPFYLLER_KRAV));
    }

    @Test void skalHenteSisteArbeidsforhold() throws Exception {
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        Arbeidsforhold arbeidsforhold = startRegistreringStatusResolver.hentArbeidsforholdet(FNR_OPPFYLLER_KRAV);
        assertThat(arbeidsforhold.getStyrk()).isEqualTo("styrk");
    }

    private ArenaOppfolging underOppfolgingIArena() {
        return new ArenaOppfolging().setFormidlingsgruppe("ARBS").setServicegruppe("BATT");
    }

    private ArenaOppfolging arenaISERV(LocalDate iservFra) {
        return new ArenaOppfolging().setFormidlingsgruppe("ISERV").setServicegruppe("IVURD").setInaktiveringsdato(iservFra);
    }

    @SneakyThrows
    private StartRegistreringStatus getStartRegistreringStatus(String fnr) {
        return startRegistreringStatusResolver.hentStartRegistreringStatus(fnr);
    }

    private List<Arbeidsforhold> arbeidsforholdSomOppfyllerKrav() {
        return Collections.singletonList(new Arbeidsforhold()
                .setArbeidsgiverOrgnummer("orgnummer")
                .setStyrk("styrk")
                .setFom(LocalDate.of(2017,1,10)));
    }


    private void mockOppfolgingsflagg() {
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
    }

    @SneakyThrows
    private void mockArbeidsforhold(List<Arbeidsforhold> arbeidsforhold) {
        when(arbeidsforholdService.hentArbeidsforhold(any())).thenReturn(arbeidsforhold);
    }

    private void mockFinnesIkkeIArena() {
        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenThrow(NotFoundException.class);
    }

    private void mockArenaMedRespons(ArenaOppfolging arenaOppfolging){
        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(arenaOppfolging);
    }


}