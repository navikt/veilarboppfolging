package no.nav.fo.veilarboppfolging.services.startregistrering;

import lombok.SneakyThrows;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.Arbeidsforhold;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.fo.veilarboppfolging.services.ArbeidsforholdService;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.registrerBruker.StartRegistreringStatusResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.NotFoundException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarboppfolging.TestUtils.getFodselsnummerForPersonWithAge;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.MAX_ALDER_AUTOMATISK_REGISTRERING;
import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtils.MIN_ALDER_AUTOMATISK_REGISTRERING;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StartRegistreringStatusResolverTest {

    private PepClient pepClient;
    private ArbeidsforholdService arbeidsforholdService;

    private ArenaOppfolgingService arenaOppfolgingService;
    private StartRegistreringStatusResolver startRegistreringStatusResolver;

    private static String FNR_OPPFYLLER_KRAV = getFodselsnummerForPersonWithAge(40);
    private static String FNR_OPPFYLLER_IKKE_KRAV = getFodselsnummerForPersonWithAge(20);


    @BeforeEach
    public void setup() {
        pepClient = mock(PepClient.class);
        arbeidsforholdService = mock(ArbeidsforholdService.class);
        arenaOppfolgingService = mock(ArenaOppfolgingService.class);
        startRegistreringStatusResolver =
                new StartRegistreringStatusResolver(
                        pepClient,
                        arenaOppfolgingService,
                        arbeidsforholdService
                );

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
    public void skalKalleArena() {
        mockFinnesIkkeIArena();
        getStartRegistreringStatus(FNR_OPPFYLLER_KRAV);
        verify(arenaOppfolgingService, timeout(1)).hentArenaOppfolging(any());
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
        System.setProperty(MIN_ALDER_AUTOMATISK_REGISTRERING, "30");
        System.setProperty(MAX_ALDER_AUTOMATISK_REGISTRERING, "59");
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
        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> getStartRegistreringStatus(FNR_OPPFYLLER_KRAV));
    }

    @Test void skalHenteSisteArbeidsforhold() throws Exception {
        mockArbeidsforhold(arbeidsforholdSomOppfyllerKrav());
        Arbeidsforhold arbeidsforhold = startRegistreringStatusResolver.hentSisteArbeidsforhold(FNR_OPPFYLLER_KRAV);
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