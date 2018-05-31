package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.OppfolgingStatusData;
import no.nav.fo.veilarboppfolging.rest.domain.AktivStatus;
import no.nav.fo.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.NotFoundException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AktivStatusRessursTest {

    @Mock
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    private OppfolgingService oppfolgingService;

    @Mock
    private PepClient pepClient;

    @Test
    public void skalReturnereVerdiSelvOmBrukerIkkeFinnesIArena() throws Exception {

        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenThrow(NotFoundException.class);
        when(oppfolgingService.hentOppfolgingsStatus(anyString())).thenReturn(createBrukerMedOppfolgingsflagg());

        AktivStatusRessurs aktivStatusRessurs = new AktivStatusRessurs(arenaOppfolgingService, pepClient, oppfolgingService);
        AktivStatus aktivStatus = aktivStatusRessurs.getAggregertAktivStatus("fnr");

        assertNotNull(aktivStatus);
    }

    @Test
    public void skalReturnereVerdiSelvOmBrukerIkkeFinnesITPS() throws Exception {

        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(createBrukerIArena("ARBS", false));
        when(oppfolgingService.hentOppfolgingsStatus(anyString())).thenThrow(Exception.class);

        AktivStatusRessurs aktivStatusRessurs = new AktivStatusRessurs(arenaOppfolgingService, pepClient, oppfolgingService);
        AktivStatus aktivStatus = aktivStatusRessurs.getAggregertAktivStatus("fnr");

        assertNotNull(aktivStatus);
    }

    @Test
    public void aktivBrukerIArenaMedOppfolgingsflaggHarAktivStatus() throws Exception {

        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(createBrukerIArena("ARBS", false));
        when(oppfolgingService.hentOppfolgingsStatus(anyString())).thenReturn(createBrukerMedOppfolgingsflagg());

        AktivStatusRessurs aktivStatusRessurs = new AktivStatusRessurs(arenaOppfolgingService, pepClient, oppfolgingService);
        AktivStatus aktivStatus = aktivStatusRessurs.getAggregertAktivStatus("fnr");

        assertThat(aktivStatus.isAktiv(), is(true));
        assertThat(aktivStatus.isUnderOppfolging(), is(true));
    }

    @Test
    public void aktivBrukerIArenaUtenOppfolgingsflaggHarAktivStatus() throws Exception {

        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(createBrukerIArena("ARBS", false));
        when(oppfolgingService.hentOppfolgingsStatus(anyString())).thenReturn(createBrukerUtenOppfolgingsflagg());

        AktivStatusRessurs aktivStatusRessurs = new AktivStatusRessurs(arenaOppfolgingService, pepClient, oppfolgingService);
        AktivStatus aktivStatus = aktivStatusRessurs.getAggregertAktivStatus("fnr");

        assertThat(aktivStatus.isAktiv(), is(true));
        assertThat(aktivStatus.isUnderOppfolging(), is(false));
    }

    @Test
    public void inaktivBrukerIArenaMedOppfolgingsflaggHarInaktivStatus() throws Exception {

        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(createBrukerIArena("ISERV", false));
        when(oppfolgingService.hentOppfolgingsStatus(anyString())).thenReturn(createBrukerMedOppfolgingsflagg());

        AktivStatusRessurs aktivStatusRessurs = new AktivStatusRessurs(arenaOppfolgingService, pepClient, oppfolgingService);
        AktivStatus aktivStatus = aktivStatusRessurs.getAggregertAktivStatus("fnr");

        assertThat(aktivStatus.isAktiv(), is(false));
        assertThat(aktivStatus.isUnderOppfolging(), is(true));
    }

    @Test
    public void reaktiveringsBrukerIArenaMedOppfolgingsflaggHarInaktivStatus() throws Exception {

        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(createBrukerIArena("RARBS", false));
        when(oppfolgingService.hentOppfolgingsStatus(anyString())).thenReturn(createBrukerMedOppfolgingsflagg());

        AktivStatusRessurs aktivStatusRessurs = new AktivStatusRessurs(arenaOppfolgingService, pepClient, oppfolgingService);
        AktivStatus aktivStatus = aktivStatusRessurs.getAggregertAktivStatus("fnr");

        assertThat(aktivStatus.isAktiv(), is(false));
        assertThat(aktivStatus.isUnderOppfolging(), is(true));
    }

    @Test
    public void preArbeidssokerBrukerIArenaMedOppfolgingsflaggHarInaktivStatus() throws Exception {

        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenReturn(createBrukerIArena("PARBS", false));
        when(oppfolgingService.hentOppfolgingsStatus(anyString())).thenReturn(createBrukerMedOppfolgingsflagg());

        AktivStatusRessurs aktivStatusRessurs = new AktivStatusRessurs(arenaOppfolgingService, pepClient, oppfolgingService);
        AktivStatus aktivStatus = aktivStatusRessurs.getAggregertAktivStatus("fnr");

        assertThat(aktivStatus.isAktiv(), is(false));
        assertThat(aktivStatus.isUnderOppfolging(), is(true));
    }

    private OppfolgingStatusData createBrukerMedOppfolgingsflagg() {
        OppfolgingStatusData oppfolgingStatusData = new OppfolgingStatusData();
        oppfolgingStatusData.setUnderOppfolging(true);
        return oppfolgingStatusData;
    }

    private OppfolgingStatusData createBrukerUtenOppfolgingsflagg() {
        OppfolgingStatusData oppfolgingStatusData = new OppfolgingStatusData();
        oppfolgingStatusData.setUnderOppfolging(false);
        return oppfolgingStatusData;
    }


    private ArenaOppfolging createBrukerIArena(String fgKode, boolean harOppgave) {
        ArenaOppfolging arenaOppfolging = new ArenaOppfolging();
        arenaOppfolging.setFormidlingsgruppe(fgKode);
        arenaOppfolging.setServicegruppe("VURDI");
        arenaOppfolging.setHarMottaOppgaveIArena(harOppgave);
        return arenaOppfolging;
    }

}
