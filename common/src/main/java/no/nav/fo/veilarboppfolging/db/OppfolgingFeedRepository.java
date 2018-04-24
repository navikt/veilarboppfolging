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

    public List<OppfolgingFeedDTO> hentTilordningerEtterTimestamp(Timestamp timestamp, int pageSize) {
        return db.queryForList("SELECT * FROM "
                        + "(SELECT o.aktor_id, o.veileder, o.under_oppfolging, o.ny_for_veileder, o.oppdatert, m.manuell FROM OPPFOLGINGSTATUS o LEFT JOIN MANUELL_STATUS m ON (o.AKTOR_ID = m.AKTOR_ID) where o.oppdatert >= ? AND m.opprettet_av != 'SYSTEM' ORDER BY o.oppdatert) "
                        + "WHERE rownum <= ?",
                timestamp,
                pageSize
        ).stream()
                .map(OppfolgingFeedUtil::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }
}
