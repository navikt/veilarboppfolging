package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.AvsluttetOppfolgingFeedData;
import no.nav.veilarboppfolging.domain.Oppfolgingsperiode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.UNDER_OPPFOLGING;
import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;
import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;

@Repository
public class OppfolgingsPeriodeRepository {

    private final JdbcTemplate db;

    private final static String hentOppfolingsperioderSQL =
            "SELECT aktor_id, avslutt_veileder, startdato, sluttdato, avslutt_begrunnelse " +
                    "FROM OPPFOLGINGSPERIODE ";

    @Autowired
    public OppfolgingsPeriodeRepository(JdbcTemplate db) {
        this.db = db;
    }

    @Transactional
    public void start(String aktorId) {
        insert(aktorId);
        setActive(aktorId);
    }

    @Transactional
    public void avslutt(String aktorId, String veileder, String begrunnelse) {
        endPeriode(aktorId, veileder, begrunnelse);
        avsluttOppfolging(aktorId);
    }

    public List<AvsluttetOppfolgingFeedData> fetchAvsluttetEtterDato(Timestamp timestamp, int pageSize) {
        return db
                .query("SELECT * FROM (SELECT aktor_id, sluttdato, oppdatert " +
                                "FROM OPPFOLGINGSPERIODE " +
                                "WHERE oppdatert >= ? and sluttdato is not null order by oppdatert) " +
                                "WHERE rownum <= ?",
                        this::mapRadTilAvsluttetOppfolging,
                        timestamp,
                        pageSize);
    }

    public Optional<Oppfolgingsperiode> hentGjeldendeOppfolgingsperiode(String aktorId) {
        return queryForNullableObject(
                db,
                hentOppfolingsperioderSQL +
                        "WHERE aktor_id = ? AND sluttdato IS NULL " +
                        "ORDER BY startdato DESC " +
                        "FETCH NEXT 1 ROWS ONLY",
                OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode,
                aktorId
        );
    }

    public List<Oppfolgingsperiode> hentOppfolgingsperioder(String aktorId) {
        return db.query(hentOppfolingsperioderSQL +
                        "WHERE aktor_id = ?",
                OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode,
                aktorId
        );
    }

    public List<Oppfolgingsperiode> hentAvsluttetOppfolgingsperioder(String aktorId) {
        return db.query(hentOppfolingsperioderSQL +
                        "WHERE aktor_id = ? AND sluttdato is not null",
                OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode,
                aktorId
        );
    }

    private void insert(String aktorId) {
        db.update("" +
                        "INSERT INTO OPPFOLGINGSPERIODE(aktor_id, startDato, oppdatert) " +
                        "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                aktorId);
    }

    private void setActive(String aktorId) {
        db.update("UPDATE " +
                        OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + UNDER_OPPFOLGING + "= 1, " +
                        "oppdatert = CURRENT_TIMESTAMP, " +
                        "FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                aktorId);
    }

    private void endPeriode(String aktorId, String veileder, String begrunnelse) {
        db.update("" +
                        "UPDATE OPPFOLGINGSPERIODE " +
                        "SET avslutt_veileder = ?, " +
                        "avslutt_begrunnelse = ?, " +
                        "sluttDato = CURRENT_TIMESTAMP, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ? " +
                        "AND sluttDato IS NULL",
                veileder,
                begrunnelse,
                aktorId);
    }

    private void avsluttOppfolging(String aktorId) {
        db.update("UPDATE " +
                        OppfolgingsStatusRepository.TABLE_NAME +
                        " SET under_oppfolging = 0, "
                        + "veileder = null, "
                        + "ny_for_veileder = 0, "
                        + "gjeldende_manuell_status = null, "
                        + "gjeldende_mal = null, "
                        + "oppdatert = CURRENT_TIMESTAMP, "
                        + "FEED_ID = null "
                        + "WHERE aktor_id = ?",
                aktorId
        );
    }

    private static Oppfolgingsperiode mapTilOppfolgingsperiode(ResultSet result, int row) throws SQLException {
        return Oppfolgingsperiode.builder()
                .aktorId(result.getString("aktor_id"))
                .veileder(result.getString("avslutt_veileder"))
                .startDato(hentZonedDateTime(result, "startdato"))
                .sluttDato(hentZonedDateTime(result, "sluttdato"))
                .begrunnelse(result.getString("avslutt_begrunnelse"))
                .build();
    }

    private AvsluttetOppfolgingFeedData mapRadTilAvsluttetOppfolging(ResultSet rs, int row) throws SQLException {
        return AvsluttetOppfolgingFeedData.builder()
                .aktoerid(rs.getString("aktor_id"))
                .sluttdato(hentZonedDateTime(rs, "sluttdato"))
                .oppdatert(hentZonedDateTime(rs, "oppdatert"))
                .build();
    }

}
