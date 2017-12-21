package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.Brukervilkar;
import no.nav.fo.veilarboppfolging.domain.VilkarStatus;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository.GJELDENDE_BRUKERVILKAR;

public class BrukervilkarRepository {

    private Database database;

    public BrukervilkarRepository(Database database) {
        this.database = database;
    }

    @Transactional
    public void create(Brukervilkar vilkar) {
        vilkar.setId(database.nesteFraSekvens("brukervilkar_seq"));
        this.insert(vilkar);
        this.setActive(vilkar);
    }

    public Brukervilkar fetch(Long id) {
        String sql = "SELECT * FROM BRUKERVILKAR WHERE id = ?";
        return database.query(sql, BrukervilkarRepository::map, id).get(0);
    }

    public List<Brukervilkar> history(String aktorId) {
        String sql = "SELECT * FROM BRUKERVILKAR WHERE aktor_id = ? ORDER BY dato DESC";
        return database.query(sql, BrukervilkarRepository::map, aktorId);
    }

    private void insert(Brukervilkar vilkar) {
        database.update(
                "INSERT INTO BRUKERVILKAR(id, aktor_id, dato, vilkarstatus, tekst, hash) VALUES(?, ?, ?, ?, ?, ?)",
                vilkar.getId(),
                vilkar.getAktorId(),
                vilkar.getDato(),
                vilkar.getVilkarstatus().name(),
                vilkar.getTekst(),
                vilkar.getHash()
        );
    }

    private void setActive(Brukervilkar gjeldendeBrukervilkar) {
        database.update(
                "UPDATE " +
                        OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + GJELDENDE_BRUKERVILKAR + " = ?, " +
                        " oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE " + AKTOR_ID +" = ?",
                gjeldendeBrukervilkar.getId(),
                gjeldendeBrukervilkar.getAktorId()
        );
    }

    @SneakyThrows
    private static Brukervilkar map(ResultSet result) {
        return new Brukervilkar(
                result.getString("aktor_id"),
                result.getTimestamp("dato"),
                VilkarStatus.valueOf(result.getString("vilkarstatus")),
                result.getString("tekst"),
                result.getString("hash")
        ).setId(result.getLong("id"));
    }
}
