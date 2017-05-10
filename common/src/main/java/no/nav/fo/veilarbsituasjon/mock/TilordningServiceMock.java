package no.nav.fo.veilarbsituasjon.mock;

import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.services.TilordningService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class TilordningServiceMock implements TilordningService {
    @Override
    public List<OppfolgingBruker> hentTilordninger(LocalDateTime sinceId, int pageSize) {
        return testdata();
    }

    private List<OppfolgingBruker> testdata() {

        OppfolgingBruker tilordning1 = new OppfolgingBruker()
                .setAktoerid("***REMOVED***00")
                .setVeileder("***REMOVED***")
                .setOppfolging(true)
                .setEndretTimestamp(new Timestamp(2017, 5, 4, 0, 0, 0, 0));

        OppfolgingBruker tilordning2 = new OppfolgingBruker()
                .setAktoerid("***REMOVED***00")
                .setVeileder("***REMOVED***")
                .setOppfolging(true)
                .setEndretTimestamp(new Timestamp(2017, 5, 4, 0, 0, 0, 0));

        OppfolgingBruker tilordning3 = new OppfolgingBruker()
                .setAktoerid("***REMOVED***00")
                .setVeileder("***REMOVED***")
                .setOppfolging(true)
                .setEndretTimestamp(new Timestamp(2017, 5, 4, 0, 0, 0, 0));


        LinkedList<OppfolgingBruker> tilordninger = new LinkedList<>();
        tilordninger.addFirst(tilordning1);
        tilordninger.addFirst(tilordning2);
        tilordninger.addFirst(tilordning3);
        return tilordninger;
    }
}
