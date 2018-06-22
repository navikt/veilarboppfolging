package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilType;
import no.nav.fo.veilarboppfolging.domain.KodeverkBruker;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.sbl.jdbc.Database;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.apiapp.util.EnumUtils.getName;
import static no.nav.apiapp.util.EnumUtils.valueOfOptional;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.sbl.jdbc.Database.hentDato;


public class KvpRepository {

    private Database database;

    public KvpRepository(Database database) {
        this.database = database;
    }

    @Transactional
    public void startKvp(String aktorId, String enhet, String opprettetAv, String opprettetBegrunnelse) {
        if (gjeldendeKvp(aktorId) != 0) {
            throw new Feil(FeilType.UGYLDIG_REQUEST, "Aktøren er allerede under en KVP-periode.");
        }

        long id = database.nesteFraSekvens("KVP_SEQ");
        long nextSerial = database.nesteFraSekvens("KVP_SERIAL_SEQ");
        database.update("INSERT INTO KVP (" +
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
                aktorId,
                enhet,
                opprettetAv,
                opprettetBegrunnelse,
                getName(NAV)
        );
        database.update("UPDATE OPPFOLGINGSTATUS " +
                        "SET gjeldende_kvp = ?, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ?",
                id,
                aktorId
        );

    }

    public void stopKvp(String aktorId, String avsluttetAv, String avsluttetBegrunnelse, KodeverkBruker kodeverkBruker) {
        long gjeldendeKvp = gjeldendeKvp(aktorId);
        if (gjeldendeKvp == 0) {
            throw new Feil(FeilType.UGYLDIG_REQUEST, "Aktøren har ingen KVP-periode.");
        }

        long nextSerial = database.nesteFraSekvens("KVP_SERIAL_SEQ");

        database.update("UPDATE KVP " +
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
                gjeldendeKvp

        );
        database.update("UPDATE OPPFOLGINGSTATUS " +
                        "SET gjeldende_kvp = NULL, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ?",
                aktorId
        );
    }

    public List<Kvp> hentKvpHistorikk(String aktorId) {
        return database.query("SELECT * " +
                        "FROM kvp " +
                        "WHERE aktor_id = ?",
                KvpRepository::mapTilKvp,
                aktorId
        );
    }

    /**
     * Return a list of KVP objects where the serial number is greater than N.
     * The serial number is the number of updates the table has undergone.
     */
    public List<Kvp> serialGreaterThan(long serial, long pageSize) {
        return database.query("SELECT * FROM kvp WHERE serial > ? AND rownum <= ? ORDER BY serial ASC",
                KvpRepository::mapTilKvp,
                serial,
                pageSize);
    }

    public Kvp fetch(long id) {
        return database.query("SELECT * " +
                        "FROM KVP " +
                        "WHERE kvp_id = ?",
                KvpRepository::mapTilKvp, id)
                .stream()
                .findAny()
                .orElse(null);
    }

    /**
     * @param aktorId
     * @return A positive integer pointing to the KVP primary key,
     * or zero if there is no current KVP period.
     */
    public long gjeldendeKvp(String aktorId) {
        try {
            return database.queryForObject("SELECT gjeldende_kvp FROM oppfolgingstatus WHERE aktor_id = ?",
                    (rs) -> rs.getLong("gjeldende_kvp"),
                    aktorId);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    @SneakyThrows
    protected static Kvp mapTilKvp(ResultSet rs) {
        return Kvp.builder()
                .kvpId(rs.getLong("kvp_id"))
                .serial(rs.getLong("serial"))
                .aktorId(rs.getString("aktor_id"))
                .enhet(rs.getString("enhet"))
                .opprettetAv(rs.getString("opprettet_av"))
                .opprettetDato(hentDato(rs, "opprettet_dato"))
                .opprettetBegrunnelse(rs.getString("opprettet_begrunnelse"))
                .opprettetKodeverkbruker(valueOfOptional(KodeverkBruker.class,
                        rs.getString("opprettet_kodeverkbruker")).orElse(null))
                .avsluttetAv(rs.getString("avsluttet_av"))
                .avsluttetDato(hentDato(rs, "avsluttet_dato"))
                .avsluttetBegrunnelse(rs.getString("avsluttet_begrunnelse"))
                .avsluttetKodeverkbruker(valueOfOptional(KodeverkBruker.class,
                        rs.getString("avsluttet_kodeverkbruker")).orElse(null))
                .build();
    }

}
