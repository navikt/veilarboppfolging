package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.GJELDENE_ESKALERINGSVARSEL;
import static no.nav.sbl.jdbc.Database.hentDato;

@Component
public class EskaleringsvarselRepository {

    private final Database database;

    @Inject
    public EskaleringsvarselRepository(Database database) {
        this.database = database;
    }

    @Transactional
    public void create(EskaleringsvarselData e) {
        long id = database.nesteFraSekvens("ESKALERINGSVARSEL_SEQ");
        e = e.withVarselId(id);
        insert(e);
        setAcive(e);
    }

    public EskaleringsvarselData fetchByAktorId(String aktorId) {
        List<EskaleringsvarselData> eskalering = database.query("" +
                        "SELECT * FROM eskaleringsvarsel " +
                        "WHERE varsel_id IN (" +
                        "SELECT " + GJELDENE_ESKALERINGSVARSEL +
                        " FROM " + OppfolgingsStatusRepository.TABLE_NAME +
                        " WHERE " + AKTOR_ID + " = ?" +
                        ")",
                EskaleringsvarselRepository::map,
                aktorId);

        return eskalering.stream()
                .findAny()
                .orElse(null);

    }

    public EskaleringsvarselData fetch(Long id) {
        String sql = "SELECT * FROM ESKALERINGSVARSEL WHERE varsel_id = ?";
        return database.query(sql, EskaleringsvarselRepository::map, id).get(0);
    }

    @Transactional
    public void finish(EskaleringsvarselData e) {
        avsluttEskaleringsVarsel(e);
        removeActive(e);
    }


    public List<EskaleringsvarselData> history(String aktorId) {
        return database.query("SELECT * FROM ESKALERINGSVARSEL WHERE aktor_id = ?",
                EskaleringsvarselRepository::map,
                aktorId);
    }

    @SneakyThrows
    private static EskaleringsvarselData map(ResultSet result) {
        return EskaleringsvarselData.builder()
                .varselId(result.getLong("varsel_id"))
                .aktorId(result.getString("aktor_id"))
                .opprettetAv(result.getString("opprettet_av"))
                .opprettetDato(hentDato(result, "opprettet_dato"))
                .opprettetBegrunnelse(result.getString("opprettet_begrunnelse"))
                .avsluttetDato(hentDato(result, "avsluttet_dato"))
                .avsluttetBegrunnelse(result.getString( "avsluttet_begrunnelse"))
                .avsluttetAv(result.getString( "avsluttet_av"))
                .tilhorendeDialogId(result.getLong("tilhorende_dialog_id"))
                .build();
    }

    private void insert(EskaleringsvarselData e) {
        database.update("" +
                        "INSERT INTO ESKALERINGSVARSEL(" +
                        "varsel_id, " +
                        "aktor_id, " +
                        "opprettet_av, " +
                        "opprettet_dato, " +
                        "opprettet_begrunnelse, " +
                        "tilhorende_dialog_id) " +
                        "VALUES(?, ?, ?, CURRENT_TIMESTAMP, ?, ?)",
                e.getVarselId(),
                e.getAktorId(),
                e.getOpprettetAv(),
                e.getOpprettetBegrunnelse(),
                e.getTilhorendeDialogId());
    }

    private void setAcive(EskaleringsvarselData e) {
        database.update("" +
                        "UPDATE " + OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + GJELDENE_ESKALERINGSVARSEL + " = ?, " +
                        "oppdatert = CURRENT_TIMESTAMP, " +
                        "FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                e.getVarselId(),
                e.getAktorId()
        );
    }

    private void avsluttEskaleringsVarsel(EskaleringsvarselData e) {
        database.update("" +
                        "UPDATE ESKALERINGSVARSEL " +
                        "SET avsluttet_dato = CURRENT_TIMESTAMP, avsluttet_begrunnelse = ?, avsluttet_av = ? " +
                        "WHERE varsel_id = ?",
                e.getAvsluttetBegrunnelse(),
                e.getAvsluttetAv(),
                e.getVarselId());
    }

    private void removeActive(EskaleringsvarselData e) {
        database.update("" +
                        "UPDATE " + OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + GJELDENE_ESKALERINGSVARSEL + " = null, " +
                        "oppdatert = CURRENT_TIMESTAMP, " +
                        "FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                e.getAktorId()
        );
    }
}
