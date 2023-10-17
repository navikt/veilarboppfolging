package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;
import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;
import static no.nav.veilarboppfolging.utils.EnumUtils.getName;
import static no.nav.veilarboppfolging.utils.EnumUtils.valueOfOptional;

@Repository
public class KvpRepository {

    private final JdbcTemplate db;


    private final TransactionTemplate transactor;

    @Autowired
    public KvpRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
        this.transactor = transactor;
    }

    public void startKvp(AktorId aktorId, String enhet, String opprettetAv, String opprettetBegrunnelse, ZonedDateTime startDato) {
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
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?)",
                    id,
                    nextSerial,
                    aktorId.get(),
                    enhet,
                    opprettetAv,
                    startDato,
                    opprettetBegrunnelse,
                    getName(NAV)
            );

            db.update("UPDATE OPPFOLGINGSTATUS " +
                            "SET gjeldende_kvp = ?, " +
                            "oppdatert = ? " +
                            "WHERE aktor_id = ?",
                    id,
                    startDato,
                    aktorId.get()
            );
        });
    }

    public void stopKvp(long kvpId, AktorId aktorId, String avsluttetAv, String avsluttetBegrunnelse, KodeverkBruker kodeverkBruker, ZonedDateTime sluttDato) {
        transactor.executeWithoutResult((ignored) -> {
            long nextSerial = DbUtils.nesteFraSekvens(db, "KVP_SERIAL_SEQ");

            db.update("UPDATE KVP " +
                            "SET serial = ?, " +
                            "avsluttet_av = ?, " +
                            "avsluttet_dato = ?, " +
                            "avsluttet_begrunnelse = ?, " +
                            "avsluttet_kodeverkbruker = ? " +
                            "WHERE kvp_id = ?",
                    nextSerial,
                    avsluttetAv,
                    sluttDato,
                    avsluttetBegrunnelse,
                    getName(kodeverkBruker),
                    kvpId

            );

            db.update("UPDATE OPPFOLGINGSTATUS " +
                            "SET gjeldende_kvp = NULL, " +
                            "oppdatert = ? " +
                            "WHERE aktor_id = ?",
                    sluttDato,
                    aktorId.get()
            );
        });
    }

    public List<KvpPeriodeEntity> hentKvpHistorikk(AktorId aktorId) {
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
    public List<KvpPeriodeEntity> serialGreaterThan(long serial, long pageSize) {
        String sql = "SELECT * FROM kvp WHERE serial > ? AND rownum <= ? ORDER BY serial ASC";
        return db.query(sql, KvpRepository::mapTilKvp, serial, pageSize);
    }

    public Optional<KvpPeriodeEntity> hentKvpPeriode(long id) {
        String sql = "SELECT * FROM KVP WHERE kvp_id = ?";
        return queryForNullableObject(() -> db.queryForObject(sql, KvpRepository::mapTilKvp, id));
    }

    /**
     * @param aktorId
     * @return A positive integer pointing to the KVP primary key,
     * or zero if there is no current KVP period.
     */
    public long gjeldendeKvp(AktorId aktorId) {
        return queryForNullableObject(
                () -> db.queryForObject("SELECT gjeldende_kvp FROM oppfolgingstatus WHERE aktor_id = ?",
                (rs, row) -> rs.getLong("gjeldende_kvp"),
                aktorId.get())
        ).orElse(0L);
    }

    public Optional<KvpPeriodeEntity> hentGjeldendeKvpPeriode(AktorId aktorId) {
        return queryForNullableObject(
                () -> db.queryForObject("SELECT * FROM KVP WHERE aktor_id = ? AND avsluttet_dato IS NULL ORDER BY opprettet_dato FETCH NEXT 1 ROWS ONLY",
                        KvpRepository::mapTilKvp,
                        aktorId.get())
        );
    }

    @SneakyThrows
    protected static KvpPeriodeEntity mapTilKvp(ResultSet rs, int row) {
        return KvpPeriodeEntity.builder()
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
