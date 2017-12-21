package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.sbl.jdbc.Database.hentDato;

public class EskaleringsvarselRepository {

    private Database database;

    public EskaleringsvarselRepository(Database database) {
        this.database = database;
    }

    protected EskaleringsvarselData fetchByAktorId(String aktorId) {
        List<EskaleringsvarselData> eskalering = database.query("" +
                        "SELECT * FROM eskaleringsvarsel " +
                        "WHERE varsel_id IN (SELECT gjeldende_eskaleringsvarsel FROM OPPFOLGINGSTATUS WHERE aktor_id = ?)",
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

    protected void create(EskaleringsvarselData e) {
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

    protected void finish(EskaleringsvarselData e) {
        database.update("" +
                        "UPDATE ESKALERINGSVARSEL " +
                        "SET avsluttet_dato = CURRENT_TIMESTAMP, avsluttet_begrunnelse = ?, avsluttet_av = ? " +
                        "WHERE varsel_id = ?",
                e.getAvsluttetBegrunnelse(),
                e.getAvsluttetAv(),
                e.getVarselId());
    }

    public List<EskaleringsvarselData> history(String aktorId) {
        return database.query("SELECT * FROM ESKALERINGSVARSEL WHERE aktor_id = ?",
                EskaleringsvarselRepository::map,
                aktorId);
    }

    @SneakyThrows
    public static EskaleringsvarselData map(ResultSet result) {
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
}
