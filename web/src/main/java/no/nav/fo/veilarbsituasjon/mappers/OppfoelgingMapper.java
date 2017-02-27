package no.nav.fo.veilarbsituasjon.mappers;


import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktData;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Oppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.ServiceGruppe;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OppfoelgingMapper {

    public static OppfoelgingskontraktResponse tilOppfoelgingskontrakt(HentOppfoelgingskontraktListeResponse response) {

        final List<OppfoelgingskontraktData> oppfoelgingskontrakter = response.getOppfoelgingskontraktListe().stream()
                .map(tilOppfoelgingskontrakt).collect(Collectors.toList());

        return new OppfoelgingskontraktResponse(oppfoelgingskontrakter);
    }

    private final static Function<Oppfoelgingskontrakt, OppfoelgingskontraktData> tilOppfoelgingskontrakt =
            wsOppfoelgingskontrakt -> new OppfoelgingskontraktData()
                    .withInnsatsgruppe(wsOppfoelgingskontrakt
                            .getGjelderBruker()
                            .getServicegruppe()
                            .stream()
                            .map(ServiceGruppe::getServiceGruppe).collect(Collectors.toList()))
                    .withStatus(wsOppfoelgingskontrakt.getStatus());
}
