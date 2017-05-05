package no.nav.fo.veilarbsituasjon.mock;

import no.nav.fo.veilarbsituasjon.domain.Tilordning;
import no.nav.fo.veilarbsituasjon.services.TilordningService;

import java.time.LocalDateTime;
import java.util.LinkedList;

public class TilordningServiceMock implements TilordningService {
    @Override
    public LinkedList<Tilordning> hentTilordninger(LocalDateTime sinceId) {
        return testdata();
    }

    private LinkedList<Tilordning> testdata() {

        Tilordning tilordning1 = new Tilordning()
                .setAktorId("***REMOVED***00")
                .setVeilederId("***REMOVED***")
                .setOppfolging(true)
                .setSistOppdatert("2017-05-02T15:41:00+02:00");

        Tilordning tilordning2 = new Tilordning()
                .setAktorId("***REMOVED***01")
                .setVeilederId("***REMOVED***")
                .setOppfolging(true)
                .setSistOppdatert("2017-05-03T15:41:00+02:00");

        Tilordning tilordning3 = new Tilordning()
                .setAktorId("***REMOVED***02")
                .setVeilederId("***REMOVED***")
                .setOppfolging(true)
                .setSistOppdatert("2017-05-04T15:41:00+02:00");


        LinkedList<Tilordning> tilordninger = new LinkedList<>();
        tilordninger.addFirst(tilordning1);
        tilordninger.addFirst(tilordning2);
        tilordninger.addFirst(tilordning3);
        return tilordninger;
    }
}
