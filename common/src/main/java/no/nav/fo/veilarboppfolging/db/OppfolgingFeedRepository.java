package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.utils.OppfolgingFeedUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class OppfolgingFeedRepository {

    private JdbcTemplate db;

    public OppfolgingFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<OppfolgingFeedDTO> hentTilordningerEtterTimestamp(Timestamp timestamp) {
        return db.queryForList("SELECT AKTORID, VEILEDER, OPPFOLGING, OPPDATERT " +
                        "FROM SITUASJON " +
                        "WHERE OPPDATERT >= ? ",
                timestamp)
                .stream()
                .map(OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }
}
