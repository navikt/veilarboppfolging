package no.nav.veilarboppfolging.utils.auth;

import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput;
import org.mockito.ArgumentMatcher;

import java.util.UUID;

public class PolicyInputMatcher implements ArgumentMatcher<NavAnsattTilgangTilEksternBrukerPolicyInput> {

    private final UUID veilderId;
    private final String fnr;

    public PolicyInputMatcher(UUID veilderId, String fnr) {
        this.veilderId = veilderId;
        this.fnr = fnr;
    }

    @Override
    public boolean matches(NavAnsattTilgangTilEksternBrukerPolicyInput navAnsattTilgangTilEksternBrukerPolicyInput) {
        boolean riktigVeileder = navAnsattTilgangTilEksternBrukerPolicyInput.getNavAnsattAzureId().equals(veilderId);
        boolean riktigBruker = navAnsattTilgangTilEksternBrukerPolicyInput.getNorskIdent().equals(fnr);
        return riktigVeileder && riktigBruker;
    }
}
