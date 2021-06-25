package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.repository.entity.EskaleringsvarselEntity;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.GJELDENE_ESKALERINGSVARSEL;
import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;
import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;

@Repository
public class EskaleringsvarselRepository {

    private final JdbcTemplate db;

    private final TransactionTemplate transactor;

    @Autowired
    public EskaleringsvarselRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
        this.transactor = transactor;
    }

    public void create(EskaleringsvarselEntity e) {
        transactor.executeWithoutResult((ignored) -> {
            long id = DbUtils.nesteFraSekvens(db, "ESKALERINGSVARSEL_SEQ");
            EskaleringsvarselEntity varsel = e.withVarselId(id);
            insert(varsel);
            setActive(varsel);
        });
    }

    public Optional<EskaleringsvarselEntity> hentEskaleringsvarsel(Long id) {
        String sql = "SELECT * FROM ESKALERINGSVARSEL WHERE varsel_id = ?";
        return queryForNullableObject(() -> db.queryForObject(sql, EskaleringsvarselRepository::map, id));
    }

    public void finish(AktorId aktorId, long varselId, String avsluttetAv, String avsluttetBegrunnelse, ZonedDateTime sluttDato) {
        transactor.executeWithoutResult((ignored) -> {
            avsluttEskaleringsVarsel(avsluttetBegrunnelse, avsluttetAv, varselId, sluttDato);
            removeActive(aktorId, sluttDato);
        });
    }


    public List<EskaleringsvarselEntity> history(AktorId aktorId) {
        return db.query("SELECT * FROM ESKALERINGSVARSEL WHERE aktor_id = ?",
                EskaleringsvarselRepository::map,
                aktorId.get());
    }

    @SneakyThrows
    private static EskaleringsvarselEntity map(ResultSet result, int row) {
        return EskaleringsvarselEntity.builder()
                .varselId(result.getLong("varsel_id"))
                .aktorId(result.getString("aktor_id"))
                .opprettetAv(result.getString("opprettet_av"))
                .opprettetDato(hentZonedDateTime(result, "opprettet_dato"))
                .opprettetBegrunnelse(result.getString("opprettet_begrunnelse"))
                .avsluttetDato(hentZonedDateTime(result, "avsluttet_dato"))
                .avsluttetBegrunnelse(result.getString( "avsluttet_begrunnelse"))
                .avsluttetAv(result.getString( "avsluttet_av"))
                .tilhorendeDialogId(result.getLong("tilhorende_dialog_id"))
                .build();
    }

    private void insert(EskaleringsvarselEntity e) {
        String sql = "INSERT INTO ESKALERINGSVARSEL" +
                "(varsel_id, aktor_id, opprettet_av, opprettet_dato, opprettet_begrunnelse, tilhorende_dialog_id)" +
                " VALUES(?, ?, ?, CURRENT_TIMESTAMP, ?, ?)";

        db.update(sql, e.getVarselId(), e.getAktorId(), e.getOpprettetAv(), e.getOpprettetBegrunnelse(), e.getTilhorendeDialogId());
    }

    private void setActive(EskaleringsvarselEntity e) {
        db.update("" +
                        "UPDATE " + OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + GJELDENE_ESKALERINGSVARSEL + " = ?, " +
                        "oppdatert = CURRENT_TIMESTAMP, " +
                        "FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                e.getVarselId(),
                e.getAktorId()
        );
    }

    void avsluttEskaleringsVarsel(String avsluttetBegrunnelse, String avsluttetAv, long varselId, ZonedDateTime sluttDato) {
        db.update("" +
                        "UPDATE ESKALERINGSVARSEL " +
                        "SET avsluttet_dato = ?, avsluttet_begrunnelse = ?, avsluttet_av = ? " +
                        "WHERE varsel_id = ?",
                sluttDato,
                avsluttetBegrunnelse,
                avsluttetAv,
                varselId);
    }

    private void removeActive(AktorId aktorId, ZonedDateTime oppdatert) {
        db.update("" +
                        "UPDATE " + OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + GJELDENE_ESKALERINGSVARSEL + " = null, " +
                        "oppdatert = ?, " +
                        "FEED_ID = null " +
                        "WHERE " + AKTOR_ID + " = ?",
                oppdatert,
                aktorId.get()
        );
    }
}
