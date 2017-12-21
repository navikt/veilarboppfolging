package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.KodeverkBruker;
import no.nav.fo.veilarboppfolging.domain.ManuellStatus;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.apiapp.util.EnumUtils.getName;
import static no.nav.apiapp.util.EnumUtils.valueOfOptional;

public class ManuellStatusRepository {

    private Database database;

    public ManuellStatusRepository(Database database) {
        this.database = database;
    }

    protected void create(ManuellStatus manuellStatus) {
        database.update(
                "INSERT INTO MANUELL_STATUS(" +
                        "id, " +
                        "aktor_id, " +
                        "manuell, " +
                        "opprettet_dato, " +
                        "begrunnelse, " +
                        "opprettet_av, " +
                        "opprettet_av_brukerid) " +
                        "VALUES(?, ?, ?, ?, ?, ?, ?)",
                manuellStatus.getId(),
                manuellStatus.getAktorId(),
                manuellStatus.isManuell(),
                manuellStatus.getDato(),
                manuellStatus.getBegrunnelse(),
                getName(manuellStatus.getOpprettetAv()),
                manuellStatus.getOpprettetAvBrukerId()
        );
    }

    public ManuellStatus fetch(Long id) {
        String sql = "SELECT * FROM MANUELL_STATUS WHERE id = ?";
        return database.query(sql, ManuellStatusRepository::map, id).get(0);
    }

    public List<ManuellStatus> history(String aktorId) {
        return database.query("SELECT * FROM MANUELL_STATUS WHERE aktor_id = ?",
                ManuellStatusRepository::map,
                aktorId);
    }

    @SneakyThrows
    public static ManuellStatus map(ResultSet result) {
        return new ManuellStatus()
                .setId(result.getLong("id"))
                .setAktorId(result.getString("aktor_id"))
                .setManuell(result.getBoolean("manuell"))
                .setDato(result.getTimestamp("opprettet_dato"))
                .setBegrunnelse(result.getString("begrunnelse"))
                .setOpprettetAv(valueOfOptional(KodeverkBruker.class, result.getString("opprettet_av")).orElse(null))
                .setOpprettetAvBrukerId(result.getString("opprettet_av_brukerid"));
    }
}
