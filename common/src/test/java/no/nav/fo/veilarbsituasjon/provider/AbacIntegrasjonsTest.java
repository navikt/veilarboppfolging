package no.nav.fo.veilarbsituasjon.provider;

import javax.inject.Inject;

import no.nav.apiapp.security.PepClient;
import no.nav.apiapp.security.PepClientTester;
import no.nav.fo.IntegrasjonsTest;

public class AbacIntegrasjonsTest extends IntegrasjonsTest implements PepClientTester {

    @Inject
    private PepClient pepClient;

    @Override
    public PepClient getPepClient() {
        return pepClient;
    }

}