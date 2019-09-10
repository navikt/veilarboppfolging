package no.nav.fo.veilarboppfolging.db;

import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.fo.DatabaseTest;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import no.nav.sbl.jdbc.Database;
import org.junit.Test;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class OppfolgingRepositoryTest extends DatabaseTest {

    private static final String AKTOR_ID = "aktorId";
    private static final String ENHET = "enhet";
    private static final String VEILERDER = "veileder";
    private static final String BEGRUNNELSE = "begrunnelse";
    private static final String OTHER_ENHET = "otherEnhet";

    @Inject
    private VeilarbAbacPepClient pepClientMock;

    @Inject
    private KvpRepository kvpRepository;

    @Inject
    private Database db;

    @Inject
    private OppfolgingRepository oppfolgingRepository;

    @Test
    public void oppfolging_periode_uten_kvp_perioder() {
        gittOppfolgingForAktor(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(0);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder() throws PepException {
        when(pepClientMock.harTilgangTilEnhet(ENHET)).thenReturn(true);

        gittOppfolgingForAktor(AKTOR_ID);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(ENHET);
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(2);
    }

    @Test
    public void oppfolging_periode_med_kvp_perioder_bare_tilgang_til_en() throws PepException {
        when(pepClientMock.harTilgangTilEnhet(ENHET)).thenReturn(true);
        when(pepClientMock.harTilgangTilEnhet(OTHER_ENHET)).thenReturn(false);

        gittOppfolgingForAktor(AKTOR_ID);
        gitt_kvp_periode(ENHET);
        gitt_kvp_periode(OTHER_ENHET);
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();

        assertThat(oppfolging.getOppfolgingsperioder().get(0).getKvpPerioder()).hasSize(1);
    }

    @Test
    public void opprettOghentMal() {
        gittOppfolgingForAktor(AKTOR_ID);
        opprettMal(AKTOR_ID, "Dette er et mål");

        MalData mal = getGjeldendeMal(AKTOR_ID);
        assertThat(mal.getAktorId(), equalTo(AKTOR_ID));
        assertThat(mal.getMal(), equalTo("Dette er et mål"));
    }

    @Test
    public void hentMalListe() {
        gittOppfolgingForAktor(AKTOR_ID);
        opprettMal(AKTOR_ID, "Dette er et mål");
        opprettMal(AKTOR_ID, "Dette er et oppdatert mål");

        MalData mal = getGjeldendeMal(AKTOR_ID);
        assertThat(mal.getMal(), equalTo("Dette er et oppdatert mål"));
        List<MalData> malList = hentMal(AKTOR_ID);
        assertThat(malList.size(), greaterThan(1));
    }

    private MalData getGjeldendeMal(String aktorId) {
        return oppfolgingRepository.hentOppfolging(aktorId).get().getGjeldendeMal();
    }

    private List<MalData> hentMal(String aktorId) {
        return oppfolgingRepository.hentMalList(aktorId);
    }

    @Test
    public void manglerOppfolging() throws Exception {
        sjekkAtOppfolgingMangler(hentOppfolging("ukjentAktorId"));
        sjekkAtOppfolgingMangler(hentOppfolging(null));
    }

    @Test
    public void kanHenteForventetOppfolging() throws Exception {
        oppfolgingRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        Oppfolging uthentetOppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(uthentetOppfolging.getAktorId(), equalTo(AKTOR_ID));
        assertThat(uthentetOppfolging.isUnderOppfolging(), is(true));
        assertThat(uthentetOppfolging.getOppfolgingsperioder().size(), is(1));
    }

    @Test
    public void avsluttOppfolgingResetterVeileder_Manuellstatus_Mal_Og_Vilkar() throws Exception {
        oppfolgingRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        String veilederId = "veilederId";
        String maal = "Mål";
        settVeileder(veilederId, AKTOR_ID);
        oppfolgingRepository.opprettManuellStatus(
                new ManuellStatus()
                        .setAktorId(AKTOR_ID)
                        .setManuell(true)
                        .setDato(new Timestamp(currentTimeMillis()))
                        .setBegrunnelse("Test")
                        .setOpprettetAv(KodeverkBruker.SYSTEM));
        oppfolgingRepository.opprettMal(new MalData().setAktorId(AKTOR_ID).setMal(maal).setEndretAv("bruker").setDato(new Timestamp(currentTimeMillis())));
        Oppfolging oppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.isUnderOppfolging(), is(true));
        assertThat(oppfolging.getVeilederId(), equalTo(veilederId));
        assertThat(oppfolging.getGjeldendeManuellStatus().isManuell(), is(true));
        assertThat(oppfolging.getGjeldendeMal().getMal(), equalTo(maal));

        oppfolgingRepository.avsluttOppfolging(AKTOR_ID, veilederId, "Funnet arbeid");
        Oppfolging avsluttetOppfolging = hentOppfolging(AKTOR_ID).get();
        assertThat(avsluttetOppfolging.isUnderOppfolging(), is(false));
        assertThat(avsluttetOppfolging.getVeilederId(), nullValue());
        assertThat(avsluttetOppfolging.getGjeldendeManuellStatus(), nullValue());
        assertThat(avsluttetOppfolging.getGjeldendeMal(), nullValue());

        List<Oppfolgingsperiode> oppfolgingsperioder = avsluttetOppfolging.getOppfolgingsperioder();
        assertThat(oppfolgingsperioder.size(), is(1));


    }


    @Test
    public void kanHenteOppfolgingMedIngenOppfolgingsperioder() throws Exception {
        oppfolgingRepository.opprettOppfolging(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getOppfolgingsperioder(), empty());
    }

    @Test
    public void kanHenteOppfolgingMedOppfolgingsperioder() throws Exception {
        oppfolgingRepository.opprettOppfolging(AKTOR_ID);
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        List<Oppfolgingsperiode> oppfolgingsperioder = oppfolgingRepository.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getStartDato(), not(nullValue()));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), nullValue());

        oppfolgingRepository.avsluttOppfolging(AKTOR_ID, "veileder", "begrunnelse");
        oppfolgingsperioder = oppfolgingRepository.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(1));
        assertThat(oppfolgingsperioder.get(0).getSluttDato(), not(nullValue()));

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(AKTOR_ID);
        oppfolgingsperioder = oppfolgingRepository.hentOppfolging(AKTOR_ID).get().getOppfolgingsperioder();
        assertThat(oppfolgingsperioder, hasSize(2));
    }


    @Test
    public void utenVeileder() throws Exception {
        oppfolgingRepository.opprettOppfolging(AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getVeilederId(), nullValue());
    }

    @Test
    public void medVeilederPaaNyBruker() throws Exception {
        String veilederId = "veilederId";
        oppfolgingRepository.opprettOppfolging(AKTOR_ID);
        settVeileder(veilederId, AKTOR_ID);
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(AKTOR_ID).get();
        assertThat(oppfolging.getVeilederId(), equalTo(veilederId));
    }


    //Setter veileder direkte vha. sql, siden det ikke finnes funksjonalitet for tildeling av veileder i
    //OppfolgingRepository. Dette finnes kun i OppfolgingFeedRepository (og tilbys i PortefoljeRessurs) p.t.
    //Men siden hentOppfolging henter opp veilder er det likevel aktuelt å teste her at veileder returneres
    //dersom det er satt i databasen. 
    private void settVeileder(String veilederId, String aktorId) {
        db.update("UPDATE OPPFOLGINGSTATUS SET VEILEDER = ? where aktor_id = ?", veilederId, aktorId);
    }

    private Oppfolging gittOppfolgingForAktor(String aktorId) {
        Oppfolging oppfolging = oppfolgingRepository.hentOppfolging(aktorId)
                .orElseGet(() -> oppfolgingRepository.opprettOppfolging(aktorId));

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktorId);
        oppfolging.setUnderOppfolging(true);
        return oppfolging;
    }

    private void sjekkAtOppfolgingMangler(Optional<Oppfolging> oppfolging) {
        assertThat(oppfolging.isPresent(), is(false));
    }

    private Optional<Oppfolging> hentOppfolging(String ukjentAktorId) {
        return oppfolgingRepository.hentOppfolging(ukjentAktorId);
    }

    private void opprettMal(String aktorId, String mal) {
        MalData input = new MalData()
                .setAktorId(aktorId)
                .setMal(mal)
                .setEndretAv(aktorId)
                .setDato(new Timestamp(System.currentTimeMillis()));
        oppfolgingRepository.opprettMal(input);
    }

    private void gitt_kvp_periode(String enhet) {
        kvpRepository.startKvp(AKTOR_ID, enhet, VEILERDER, BEGRUNNELSE);
        long kvpId = kvpRepository.gjeldendeKvp(AKTOR_ID);
        kvpRepository.stopKvp(kvpId, AKTOR_ID, VEILERDER, BEGRUNNELSE, NAV);
    }
}