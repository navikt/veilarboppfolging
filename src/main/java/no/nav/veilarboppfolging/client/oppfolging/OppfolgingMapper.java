package no.nav.veilarboppfolging.client.oppfolging;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Oppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.ServiceGruppe;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OppfolgingMapper {

    static List<OppfolgingskontraktData> tilOppfolgingskontrakt(HentOppfoelgingskontraktListeResponse response) {
        return response.getOppfoelgingskontraktListe().stream()
                .map(tilOppfolgingskontrakt).collect(Collectors.toList());
    }

    private final static Function<Oppfoelgingskontrakt, OppfolgingskontraktData> tilOppfolgingskontrakt =
            wsOppfolgingskontrakt -> new OppfolgingskontraktData()
                    .withInnsatsgruppe(wsOppfolgingskontrakt
                            .getGjelderBruker()
                            .getServicegruppe()
                            .stream()
                            .map(ServiceGruppe::getServiceGruppe).collect(Collectors.toList()))
                    .withStatus(wsOppfolgingskontrakt.getStatus());
}
