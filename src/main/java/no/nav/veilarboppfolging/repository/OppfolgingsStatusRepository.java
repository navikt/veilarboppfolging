package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.domain.Oppfolging;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Repository
public class OppfolgingsStatusRepository {

    static final String GJELDENE_ESKALERINGSVARSEL = "gjeldende_eskaleringsvarsel";
    static final String GJELDENDE_MAL = "gjeldende_mal";
    static final String GJELDENDE_MANUELL_STATUS = "gjeldende_manuell_status";
    static final String AKTOR_ID = "aktor_id";
    static final String UNDER_OPPFOLGING = "under_oppfolging";
    static final String TABLE_NAME = "OPPFOLGINGSTATUS";
    static final String VEILEDER = "veileder";
    static final String NY_FOR_VEILEDER = "ny_for_veileder";
    static final String SIST_TILORDNET = "sist_tilordnet";
    static final String OPPDATERT = "oppdatert";

    private final JdbcTemplate db;

    @Autowired
    public OppfolgingsStatusRepository(JdbcTemplate db) {
        this.db = db;
    }

    public OppfolgingTable fetch(AktorId aktorId) {
        List<OppfolgingTable> t = db.query(
                "SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = ?",
                OppfolgingsStatusRepository::map,
                aktorId.get()
        );

        return !t.isEmpty() ? t.get(0) : null;
    }

    public Oppfolging opprettOppfolging(AktorId aktorId) {
        db.update("INSERT INTO OPPFOLGINGSTATUS(" +
                        "aktor_id, " +
                        "under_oppfolging, " +
                        "oppdatert) " +
                        "VALUES(?, ?, CURRENT_TIMESTAMP)",
                aktorId.get(),
                false);

        // FIXME: return the actual database object.
        return new Oppfolging().setAktorId(aktorId.get()).setUnderOppfolging(false);
    }

    public List<AktorId> hentUnikeBrukerePage(int offset, int pageSize) {
        String sql = format("SELECT DISTINCT aktor_id FROM OPPFOLGINGSTATUS ORDER BY aktor_id OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, pageSize);
        return db.query(sql, (rs, rowNum) -> rs.getString("aktor_id"))
                .stream()
                .map(AktorId::of)
                .collect(Collectors.toList());
    }

    public static OppfolgingTable map(ResultSet rs, int row) throws SQLException {
        return new OppfolgingTable()
                .setAktorId(rs.getString(AKTOR_ID))
                .setGjeldendeManuellStatusId(rs.getLong(GJELDENDE_MANUELL_STATUS))
                .setGjeldendeMaalId(rs.getLong(GJELDENDE_MAL))
                .setGjeldendeEskaleringsvarselId(rs.getLong(GJELDENE_ESKALERINGSVARSEL))
                .setGjeldendeKvpId(rs.getLong("gjeldende_kvp"))
                .setVeilederId(rs.getString(VEILEDER))
                .setUnderOppfolging(rs.getBoolean(UNDER_OPPFOLGING));
    }
}
