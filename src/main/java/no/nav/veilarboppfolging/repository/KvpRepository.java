package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;
import static no.nav.veilarboppfolging.utils.EnumUtils.getName;
import static no.nav.veilarboppfolging.utils.EnumUtils.valueOfOptional;
import static no.nav.veilarboppfolging.utils.ListUtils.firstOrNull;

@Repository
public class KvpRepository {

    private final JdbcTemplate db;

    private final TransactionTemplate transactor;

    @Autowired
    public KvpRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
        this.transactor = transactor;
    }

    // TODO: Foretrekker å sende med dato istedenfor CURRENT_TIMESTAMP slik at det ikke blir en mismatch med datoen som f.eks blir brukt på kafka
    public void startKvp(AktorId aktorId, String enhet, String opprettetAv, String opprettetBegrunnelse) {
        transactor.executeWithoutResult((ignored) -> {
            long id = DbUtils.nesteFraSekvens(db, "KVP_SEQ");
            long nextSerial = DbUtils.nesteFraSekvens(db, "KVP_SERIAL_SEQ");

            db.update("INSERT INTO KVP (" +
                            "kvp_id, " +
                            "serial, " +
                            "aktor_id, " +
                            "enhet, " +
                            "opprettet_av, " +
                            "opprettet_dato, " +
                            "opprettet_begrunnelse, " +
                            "opprettet_kodeverkbruker) " +
                            "VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)",
                    id,
                    nextSerial,
                    aktorId.get(),
                    enhet,
                    opprettetAv,
                    opprettetBegrunnelse,
                    getName(NAV)
            );

            db.update("UPDATE OPPFOLGINGSTATUS " +
                            "SET gjeldende_kvp = ?, " +
                            "oppdatert = CURRENT_TIMESTAMP, " +
                            "FEED_ID = null " +
                            "WHERE aktor_id = ?",
                    id,
                    aktorId.get()
            );
        });
    }

    // TODO: Foretrekker å sende med dato istedenfor CURRENT_TIMESTAMP slik at det ikke blir en mismatch med datoen som f.eks blir brukt på kafka
    public void stopKvp(long kvpId, AktorId aktorId, String avsluttetAv, String avsluttetBegrunnelse, KodeverkBruker kodeverkBruker) {
        transactor.executeWithoutResult((ignored) -> {
            long nextSerial = DbUtils.nesteFraSekvens(db, "KVP_SERIAL_SEQ");

            db.update("UPDATE KVP " +
                            "SET serial = ?, " +
                            "avsluttet_av = ?, " +
                            "avsluttet_dato = CURRENT_TIMESTAMP, " +
                            "avsluttet_begrunnelse = ?, " +
                            "avsluttet_kodeverkbruker = ? " +
                            "WHERE kvp_id = ?",
                    nextSerial,
                    avsluttetAv,
                    avsluttetBegrunnelse,
                    getName(kodeverkBruker),
                    kvpId

            );

            db.update("UPDATE OPPFOLGINGSTATUS " +
                            "SET gjeldende_kvp = NULL, " +
                            "oppdatert = CURRENT_TIMESTAMP, " +
                            "FEED_ID = null " +
                            "WHERE aktor_id = ?",
                    aktorId.get()
            );
        });
    }

    public List<Kvp> hentKvpHistorikk(AktorId aktorId) {
        return db.query("SELECT * " +
                        "FROM kvp " +
                        "WHERE aktor_id = ?",
                KvpRepository::mapTilKvp,
                aktorId.get()
        );
    }

    /**
     * Return a list of KVP objects where the serial number is greater than N.
     * The serial number is the number of updates the table has undergone.
     */
    public List<Kvp> serialGreaterThan(long serial, long pageSize) {
        String sql = "SELECT * FROM kvp WHERE serial > ? AND rownum <= ? ORDER BY serial ASC";
        return db.query(sql, KvpRepository::mapTilKvp, serial, pageSize);
    }

    public Kvp fetch(long id) {
        String sql = "SELECT * FROM KVP WHERE kvp_id = ?";
        return firstOrNull(db.query(sql, KvpRepository::mapTilKvp, id));
    }

    /**
     * @param aktorId
     * @return A positive integer pointing to the KVP primary key,
     * or zero if there is no current KVP period.
     */
    public long gjeldendeKvp(AktorId aktorId) {
        try {
            return db.queryForObject("SELECT gjeldende_kvp FROM oppfolgingstatus WHERE aktor_id = ?",
                    (rs, row) -> rs.getLong("gjeldende_kvp"),
                    aktorId.get());
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @SneakyThrows
    protected static Kvp mapTilKvp(ResultSet rs, int row) {
        return Kvp.builder()
                .kvpId(rs.getLong("kvp_id"))
                .serial(rs.getLong("serial"))
                .aktorId(rs.getString("aktor_id"))
                .enhet(rs.getString("enhet"))
                .opprettetAv(rs.getString("opprettet_av"))
                .opprettetDato(hentZonedDateTime(rs, "opprettet_dato"))
                .opprettetBegrunnelse(rs.getString("opprettet_begrunnelse"))
                .opprettetKodeverkbruker(valueOfOptional(KodeverkBruker.class,
                        rs.getString("opprettet_kodeverkbruker")).orElse(null))
                .avsluttetAv(rs.getString("avsluttet_av"))
                .avsluttetDato(hentZonedDateTime(rs, "avsluttet_dato"))
                .avsluttetBegrunnelse(rs.getString("avsluttet_begrunnelse"))
                .avsluttetKodeverkbruker(valueOfOptional(KodeverkBruker.class,
                        rs.getString("avsluttet_kodeverkbruker")).orElse(null))
                .build();
    }

}
