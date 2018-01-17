package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.Feil;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.sbl.jdbc.Database.hentDato;


public class KvpRepository {

    private Database database;

    public KvpRepository(Database database) {
        this.database = database;
    }

    @Transactional
    public void startKvp(String aktorId, String enhet, String opprettetAv, String opprettetBegrunnelse) {
        if (gjeldendeKvp(aktorId) != 0) {
            throw new Feil(Feil.Type.UGYLDIG_REQUEST, "Aktøren er allerede under en KVP-periode.");
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
                        "opprettet_begrunnelse) " +
                        "VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
                id,
                nextSerial,
                aktorId,
                enhet,
                opprettetAv,
                opprettetBegrunnelse
        );
        database.update("UPDATE OPPFOLGINGSTATUS " +
                        "SET gjeldende_kvp = ?, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ?",
                id,
                aktorId
        );

    }

    public void stopKvp(String aktorId, String avsluttetAv, String avsluttetBegrunnelse) {
        long gjeldendeKvp = gjeldendeKvp(aktorId);
        if (gjeldendeKvp == 0) {
            throw new Feil(Feil.Type.UGYLDIG_REQUEST, "Aktøren har ingen KVP-periode.");
        }

        database.update("UPDATE KVP " +
                        "SET serial = KVP_SERIAL_SEQ.nextval, " +
                        "avsluttet_av = ?, " +
                        "avsluttet_dato = CURRENT_TIMESTAMP, " +
                        "avsluttet_begrunnelse = ? " +
                        "WHERE kvp_id = ?",
                avsluttetAv,
                avsluttetBegrunnelse,
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

    public Kvp fetch(long id) {
        return database.query("SELECT * " +
                        "FROM KVP " +
                        "WHERE kvp_id = ?",
                KvpRepository::mapTilKvp, id)
                .stream()
                .findAny()
                .orElse(null);
    }

    private long gjeldendeKvp(String aktorId) {
        return database.queryForObject("SELECT gjeldende_kvp FROM oppfolgingstatus WHERE aktor_id = ?",
                (rs) -> rs.getLong("gjeldende_kvp"),
                aktorId);
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
                .avsluttetAv(rs.getString("avsluttet_av"))
                .avsluttetDato(hentDato(rs, "avsluttet_dato"))
                .avsluttetBegrunnelse(rs.getString("avsluttet_begrunnelse"))
                .build();
    }

}
