package no.nav.fo.veilarbsituasjon.mappers;


import no.nav.fo.veilarbsituasjon.rest.domain.Oppfoelgingskontrakt;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfoelgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSOppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSServiceGruppe;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OppfoelgingMapper {

    public static OppfoelgingskontraktResponse tilOppfoelgingskontrakt(WSHentOppfoelgingskontraktListeResponse response) {

        final List<Oppfoelgingskontrakt> oppfoelgingskontrakter = response.getOppfoelgingskontraktListe().stream()
                .map(tilOppfoelgingskontrakt).collect(Collectors.toList());

        return new OppfoelgingskontraktResponse(oppfoelgingskontrakter);
    }

    private final static Function<WSOppfoelgingskontrakt, Oppfoelgingskontrakt> tilOppfoelgingskontrakt =
            wsOppfoelgingskontrakt -> new Oppfoelgingskontrakt()
                    .withInnsatsgruppe(wsOppfoelgingskontrakt
                            .getGjelderBruker()
                            .getServicegruppe()
                            .stream()
                            .map(WSServiceGruppe::getServiceGruppe).collect(Collectors.toList()))
                    .withStatus(wsOppfoelgingskontrakt.getStatus());
}
