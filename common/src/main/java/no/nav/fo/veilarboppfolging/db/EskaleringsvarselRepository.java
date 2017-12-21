package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.GJELDENE_ESKALERINGSVARSEL;
import static no.nav.sbl.jdbc.Database.hentDato;

public class EskaleringsvarselRepository {

    private Database database;

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
                        "SELECT " +
                        "varsel_id AS esk_id, " +
                        "aktor_id AS esk_aktor_id, " +
                        "opprettet_av AS esk_opprettet_av, " +
                        "opprettet_dato AS esk_opprettet_dato, " +
                        "avsluttet_dato AS esk_avsluttet_dato, " +
                        "avsluttet_av AS esk_avsluttet_av, " +
                        "tilhorende_dialog_id AS esk_tihorende_dialog_id, " +
                        "opprettet_begrunnelse AS esk_opprettet_begrunnelse, " +
                        "avsluttet_begrunnelse AS esk_avsluttet_begrunnelse " +
                        "FROM eskaleringsvarsel " +
                        "WHERE varsel_id IN (" +
                        "SELECT " + GJELDENE_ESKALERINGSVARSEL +
                        "FROM " + OppfolgingsStatusRepository.TABLE_NAME +
                        " WHERE " + AKTOR_ID + " = ?" +
                        ")",
                EskaleringsvarselRepository::map,
                aktorId);

        return eskalering.stream()
                .findAny()
                .orElse(null);

    }

    @Transactional
    public void finish(EskaleringsvarselData e) {
        avsluttEskaleringsVarsel(e);
        removeActive(e);
    }


    public List<EskaleringsvarselData> history(String aktorId) {
        return database.query("SELECT " +
                        "varsel_id AS esk_id, " +
                        "aktor_id AS esk_aktor_id, " +
                        "opprettet_av AS esk_opprettet_av, " +
                        "opprettet_dato AS esk_opprettet_dato, " +
                        "avsluttet_av AS esk_avsluttet_av, " +
                        "avsluttet_dato AS esk_avsluttet_dato, " +
                        "tilhorende_dialog_id AS esk_tilhorende_dialog_id, " +
                        "avsluttet_begrunnelse AS esk_avsluttet_begrunnelse, " +
                        "opprettet_begrunnelse AS esk_opprettet_begrunnelse " +
                        "FROM eskaleringsvarsel " +
                        "WHERE aktor_id = ?",
                EskaleringsvarselRepository::map,
                aktorId);
    }

    @SneakyThrows
    public static EskaleringsvarselData map(ResultSet result) {
        return EskaleringsvarselData.builder()
                .varselId(result.getLong("esk_id"))
                .aktorId(result.getString("esk_aktor_id"))
                .opprettetAv(result.getString("esk_opprettet_av"))
                .opprettetDato(hentDato(result, "esk_opprettet_dato"))
                .opprettetBegrunnelse(result.getString("esk_opprettet_begrunnelse"))
                .avsluttetDato(hentDato(result, "esk_avsluttet_dato"))
                .avsluttetBegrunnelse(result.getString("esk_avsluttet_begrunnelse"))
                .avsluttetAv(result.getString("esk_avsluttet_av"))
                .tilhorendeDialogId(result.getLong("esk_tilhorende_dialog_id"))
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
                        "tilhorende_dialog_id)" +
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
                        "SET " + GJELDENE_ESKALERINGSVARSEL + " = ?, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
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
                        "SET " + GJELDENE_ESKALERINGSVARSEL + " = null, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE " + AKTOR_ID + " = ?",
                e.getAktorId()
        );
    }
}
