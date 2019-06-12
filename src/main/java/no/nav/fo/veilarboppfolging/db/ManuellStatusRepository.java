package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.KodeverkBruker;
import no.nav.fo.veilarboppfolging.domain.ManuellStatus;
import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.apiapp.util.EnumUtils.getName;
import static no.nav.apiapp.util.EnumUtils.valueOfOptional;
import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.GJELDENDE_MANUELL_STATUS;

@Component
public class ManuellStatusRepository {

    private final Database database;

    @Inject
    public ManuellStatusRepository(Database database) {
        this.database = database;
    }

    @Transactional
    public void create(ManuellStatus manuellStatus) {
        manuellStatus.setId(database.nesteFraSekvens("status_seq"));
        insert(manuellStatus);
        setActive(manuellStatus);
    }

    public ManuellStatus fetch(Long id) {
        String sql = "SELECT * FROM MANUELL_STATUS WHERE id = ?";
        List<ManuellStatus> manuellStatusList = database.query(sql, ManuellStatusRepository::map, id);
        return manuellStatusList.isEmpty() ? null : manuellStatusList.get(0);
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

    private void insert(ManuellStatus manuellStatus) {
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

    private void setActive(ManuellStatus gjeldendeManuellStatus) {
        database.update("UPDATE " +
                        OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + GJELDENDE_MANUELL_STATUS + " = ?, " +
                        "oppdatert = CURRENT_TIMESTAMP, " +
                        "FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                gjeldendeManuellStatus.getId(),
                gjeldendeManuellStatus.getAktorId()
        );
    }
}
