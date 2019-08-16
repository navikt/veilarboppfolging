package no.nav.fo.veilarboppfolging.db;


import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.Tilordning;
import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.*;

@Component
public class VeilederTilordningerRepository {

    private final Database db;
    private final OppfolgingRepository oppfolgingRepository;
    private final VeilederHistorikkRepository veilederHistorikkRepository;

    @Inject
    public VeilederTilordningerRepository(Database db, OppfolgingRepository oppfolgingRepository, VeilederHistorikkRepository veilederHistorikkRepository) {
        this.db = db;
        this.oppfolgingRepository = oppfolgingRepository;
        this.veilederHistorikkRepository = veilederHistorikkRepository;
    }

    public String hentTilordningForAktoer(String aktorId) {
        return hentTilordnetVeileder(aktorId)
                .map(Tilordning::getVeilederId)
                .orElse(null);
    }

    public Optional<Tilordning> hentTilordnetVeileder(String aktorId) {
        List<Tilordning> query = db.query("SELECT * " +
                        " FROM " + OppfolgingsStatusRepository.TABLE_NAME +
                        " WHERE " + AKTOR_ID + " = ?", this::map,
                aktorId);
        return query.stream().findAny();
    }

    @SneakyThrows
    private Tilordning map(ResultSet resultSet){
        return new Tilordning()
                .setAktorId(resultSet.getString(AKTOR_ID))
                .setOppfolging(resultSet.getBoolean(UNDER_OPPFOLGING))
                .setVeilederId(resultSet.getString(VEILEDER))
                .setNyForVeileder(resultSet.getBoolean(NY_FOR_VEILEDER))
                .setSistTilordnet(resultSet.getDate(SIST_TILORDNET))
                .setSistOppdatert(resultSet.getDate(OPPDATERT));
    }

    @Transactional
    public void upsertVeilederTilordning(String aktoerId, String veileder) {
        int rowsUpdated = db.update(
                "INSERT INTO OPPFOLGINGSTATUS(aktor_id, veileder, under_oppfolging, ny_for_veileder, sist_tilordnet, oppdatert, FEED_ID) " +
                        "SELECT ?, ?, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null FROM DUAL " +
                        "WHERE NOT EXISTS(SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id=?)",
                aktoerId, veileder, aktoerId);

        if (rowsUpdated == 0) {
            db.update(
                    "UPDATE OPPFOLGINGSTATUS SET veileder = ?, ny_for_veileder = 1, sist_tilordnet = CURRENT_TIMESTAMP, oppdatert = CURRENT_TIMESTAMP, FEED_ID = null WHERE aktor_id = ?",
                    veileder,
                    aktoerId);
        }
        veilederHistorikkRepository.insertTilordnetVeilederForAktorId(aktoerId, veileder);
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktoerId);

    }

    public int markerSomLestAvVeileder(String aktorId) {
        return db.update("UPDATE " + OppfolgingsStatusRepository.TABLE_NAME +
                " SET " + NY_FOR_VEILEDER + " = 0, " +
                OPPDATERT + " = CURRENT_TIMESTAMP, " +
                "FEED_ID = null " +
                " WHERE " + AKTOR_ID + " = ?", aktorId
        );

    }
}
