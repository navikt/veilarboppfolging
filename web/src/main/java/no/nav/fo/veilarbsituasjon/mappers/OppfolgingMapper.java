package no.nav.fo.veilarbsituasjon.mappers;


import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingskontraktData;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSOppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.WSServiceGruppe;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingskontraktListeResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OppfolgingMapper {

    public static OppfolgingskontraktResponse tilOppfolgingskontrakt(WSHentOppfoelgingskontraktListeResponse response) {

        final List<OppfolgingskontraktData> oppfolgingskontrakter = response.getOppfoelgingskontraktListe().stream()
                .map(tilOppfolgingskontrakt).collect(Collectors.toList());

        return new OppfolgingskontraktResponse(oppfolgingskontrakter);
    }

    private final static Function<WSOppfoelgingskontrakt, OppfolgingskontraktData> tilOppfolgingskontrakt =
            wsOppfolgingskontrakt -> new OppfolgingskontraktData()
                    .withInnsatsgruppe(wsOppfolgingskontrakt
                            .getGjelderBruker()
                            .getServicegruppe()
                            .stream()
                            .map(WSServiceGruppe::getServiceGruppe).collect(Collectors.toList()))
                    .withStatus(wsOppfolgingskontrakt.getStatus());
}
