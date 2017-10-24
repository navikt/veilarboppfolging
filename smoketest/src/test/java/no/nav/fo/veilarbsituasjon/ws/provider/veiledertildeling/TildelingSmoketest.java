package no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling;


import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.TokenUtils;
import no.nav.brukerdialog.security.oidc.UserTokenProvider;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsstatus;
import no.nav.fo.veilarbsituasjon.rest.domain.TilordneVeilederResponse;
import no.nav.fo.veilarbsituasjon.rest.domain.Veileder;
import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling.domene.Bruker;
import no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling.domene.Filtervalg;
import no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling.domene.Portefolje;
import no.nav.fo.veilarbsituasjon.ws.provider.veiledertildeling.domene.Statustall;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupIntegrationTestSecurity;
import static no.nav.sbl.dialogarena.test.junit.Tag.SMOKETEST;
import static no.nav.sbl.rest.RestUtils.withClient;
import static org.assertj.core.api.Java6Assertions.assertThat;

@Tag(SMOKETEST)
public class TildelingSmoketest {
    private static final String ID_TOKEN = "ID_token";

    private static final String AREMARK_FNR = "***REMOVED***";
    private static String AREMARK_OPPFOLGINGSENHET;
    private static String AREMARK_OPPFOLGINGSENHET_ANTALL;
    private static String INNLOGGET_VEILEDER;


    private static String MILJO;

    private static String TILDELING_URL;
    private static String VEILEDER_URL;
    private static String OPPFOLGINGSSTATUS_URL;
    private static String STATUSTALL_URL;
    private static String PORTEFOLJE_URL;

    private static Cookie tokenCookie;

    @BeforeAll
    public static void setup() {
        MILJO = getProperty("miljo");
        configureUrls();
        setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig("veilarbportefolje"));

        UserTokenProvider userTokenProvider = new UserTokenProvider();
        OidcCredential token = userTokenProvider.getIdToken();
        InternbrukerSubjectHandler.setOidcCredential(token);
        InternbrukerSubjectHandler.setVeilederIdent(TokenUtils.getTokenSub(token.getToken()));
        tokenCookie = new Cookie(ID_TOKEN, token.getToken());

        AREMARK_OPPFOLGINGSENHET = getOppfolgingsstatus().getOppfolgingsenhet().getEnhetId();

        Statustall statustall = getStatustall(AREMARK_OPPFOLGINGSENHET);
        long antallBrukere = statustall.getTotalt();
        AREMARK_OPPFOLGINGSENHET_ANTALL = String.valueOf(antallBrukere);

        INNLOGGET_VEILEDER = TokenUtils.getTokenSub(token.getToken());
    }

    @Test
    public void skalKunneTildeleBrukerTilVeileder() throws Exception {

        Veileder veileder = getVeileder();

        if (Objects.nonNull(veileder.getVeilederident())) {
            resetVeilederOgVerifiser(veileder.getVeilederident(), AREMARK_FNR);
        }

        tildelVeileder(null, INNLOGGET_VEILEDER, AREMARK_FNR);
        Thread.sleep(2000);
        Portefolje oppdatertPortefolje = getPortefolje(AREMARK_OPPFOLGINGSENHET, AREMARK_OPPFOLGINGSENHET_ANTALL);
        sjekkVeilederIPortefolje(oppdatertPortefolje, AREMARK_FNR, INNLOGGET_VEILEDER);
    }


    private void tildelVeileder(String fraVeileder, String tilVeileder, String fnr) {
        List<VeilederTilordning> tilordninger = new ArrayList<>();
        tilordninger.add(new VeilederTilordning()
                .setFraVeilederId(fraVeileder)
                .setTilVeilederId(tilVeileder)
                .setBrukerFnr(fnr));

        withClient((client) -> client.target(TILDELING_URL)
                .request()
                .cookie(tokenCookie)
                .post(Entity.json(tilordninger), TilordneVeilederResponse.class));
    }

    private Veileder getVeileder() {
        return withClient(client -> client.target(VEILEDER_URL)
                .request()
                .cookie(tokenCookie)
                .get(Veileder.class));
    }

    private static Oppfolgingsstatus getOppfolgingsstatus() {
        return withClient(client -> client.target(OPPFOLGINGSSTATUS_URL)
                .request()
                .cookie(tokenCookie)
                .get(Oppfolgingsstatus.class));
    }

    private static Statustall getStatustall(String enhet) {
        return withClient(client -> client.target(String.format(STATUSTALL_URL, enhet))
                .request()
                .cookie(tokenCookie)
                .get(Statustall.class));
    }

    private Portefolje getPortefolje(String enhet, String antall) {
        return withClient(client -> client.target(String.format(PORTEFOLJE_URL,enhet, antall))
        .request().cookie(tokenCookie).post(Entity.json(new Filtervalg()), Portefolje.class));
    }

    @SneakyThrows
    private static void sjekkVeilederIPortefolje(Portefolje portefolje,String fnr, String veileder) {
        Bruker bruker = portefolje.getBrukere().stream()
                .filter(b-> b.getFnr().equals(fnr)).findFirst().orElseThrow(() -> new NoSuchFieldException("Kunne ikke finne bruker i porteføljen"));

        assertThat(bruker.getVeilederId()).isEqualTo(veileder);
    }

    @SneakyThrows
    private void resetVeilederOgVerifiser(String fraVeileder, String fnr){
        tildelVeileder(fraVeileder, null, AREMARK_FNR);
        Thread.sleep(2000);
        Portefolje portefolje = getPortefolje(AREMARK_OPPFOLGINGSENHET, AREMARK_OPPFOLGINGSENHET_ANTALL);
        sjekkVeilederIPortefolje(portefolje, fnr, null);
    }

    private static void configureUrls() {
        String hostname = Objects.nonNull(MILJO) ? String.format("https://app-%s.adeo.no/", MILJO) : "http://localhost:8080/";

        STATUSTALL_URL = hostname + "veilarbportefolje/api/enhet/%s/statustall";
        PORTEFOLJE_URL = hostname + "veilarbportefolje/api/enhet/%s/portefolje" +
                "?fra=0&antall=%s&sortDirection=ikke_satt&sortField=ikke_satt";
        TILDELING_URL = hostname + "veilarbsituasjon/api/tilordneveileder";
        VEILEDER_URL = String.format(hostname + "veilarbsituasjon/api/person/%s/veileder", AREMARK_FNR);
        OPPFOLGINGSSTATUS_URL = String.format(hostname + "veilarbsituasjon/api/person/%s/oppfoelgingsstatus", AREMARK_FNR);
    }
}
