package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.KvpData;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarboppfolging.db.OppfolgingRepository.hentDato;

public class KvpRepository {

    private Database database;

    public KvpRepository(Database database) {
        this.database = database;
    }

    public void startKvp(String aktorId, String enhet, String opprettetAv, String opprettetBegrunnelse) {
        if (gjeldendeKvp(aktorId) != null) {
            throw new RuntimeException();
        }

        long id = database.nesteFraSekvens("KVP_SEQ");
        database.update("INSERT INTO KVP (" +
                        "kvp_id, " +
                        "aktor_id, " +
                        "enhet, " +
                        "opprettet_av, " +
                        "opprettet_dato, " +
                        "opprettet_begrunnelse) " +
                        "VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
                id,
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
        KvpData gjeldendeKvp = gjeldendeKvp(aktorId);
        if (gjeldendeKvp == null) {
            throw new RuntimeException();
        }

        database.update("UPDATE KVP " +
                        "SET avsluttet_av = ?, " +
                        "avsluttet_dato = CURRENT_TIMESTAMP, " +
                        "avsluttet_begrunnelse = ? " +
                        "WHERE kvp_id = ?",
                avsluttetAv,
                avsluttetBegrunnelse,
                gjeldendeKvp.getKvpId()

        );
        database.update("UPDATE OPPFOLGINGSTATUS " +
                        "SET gjeldende_kvp = NULL, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ?",
                aktorId
        );
    }

    public List<KvpData> hentKvpHistorikk(String aktorId) {
        return database.query("SELECT * " +
                        "FROM kvp " +
                        "WHERE aktor_id = ?",
                KvpRepository::mapTilKvpData,
                aktorId
        );
    }

    private KvpData gjeldendeKvp(String aktorId) {
        return database.query("SELECT * " +
                        "FROM KVP " +
                        "WHERE kvp_id IN (SELECT gjeldende_kvp FROM oppfolgingstatus WHERE aktor_id = ?)",
                KvpRepository::mapTilKvpData, aktorId)
                .stream()
                .findAny()
                .orElse(null);
    }

    @SneakyThrows
    protected static KvpData mapTilKvpData(ResultSet rs) {
        return KvpData.builder()
                .kvpId(rs.getLong("kvp_id"))
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
