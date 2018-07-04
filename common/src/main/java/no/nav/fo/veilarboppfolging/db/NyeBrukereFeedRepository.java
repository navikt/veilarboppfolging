package no.nav.fo.veilarboppfolging.db;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.domain.NyeBrukereFeedDTO;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.util.List;

@Slf4j
public class NyeBrukereFeedRepository {

    private Database database;

    public NyeBrukereFeedRepository(Database database) {
        this.database = database;
    }


    public void leggTil(Oppfolgingsbruker oppfolgingsbruker) {
        database.update(
                "INSERT INTO NYE_BRUKERE_FEED " +
                "(AKTOR_ID, SELVGAENDE) " +
                "VALUES" +
                "(?,?)", oppfolgingsbruker.getAktoerId(), false);
    }

    public Try<Integer> tryLeggTilFeedIdPaAlleElementerUtenFeedId() {
        return Try.of(this::leggTilFeedIdPaAlleElementerUtenFeedId)
                .onFailure((t) -> log.warn("Feil ved oppdatering av IDer i tabellen NYE_BRUKERE_FEED", t));
    }

    public int leggTilFeedIdPaAlleElementerUtenFeedId() {
        return database.update(
                "UPDATE NYE_BRUKERE_FEED " +
                    "SET FEED_ID = NYE_BRUKERE_FEED_SEQ.NEXTVAL " +
                    "WHERE FEED_ID IS NULL");
    }

    public List<NyeBrukereFeedDTO> hentElementerStorreEnnId(String id, int pageSize) {
        return database.query(
                "SELECT * FROM " +
                    "(SELECT * FROM NYE_BRUKERE_FEED WHERE FEED_ID > ? ORDER BY FEED_ID) " +
                    "WHERE ROWNUM <= ?", NyeBrukereFeedRepository::nyBrukerMapper, id, pageSize);
    }

    @SneakyThrows
    private static NyeBrukereFeedDTO nyBrukerMapper(ResultSet rs) {
        return NyeBrukereFeedDTO.builder()
                .id(rs.getLong("FEED_ID"))
                .aktorId(rs.getString("AKTOR_ID"))
                .selvgaende(rs.getBoolean("SELVGAENDE"))
                .opprettet(rs.getTimestamp("OPPRETTET_TIMESTAMP"))
                .build();
    }
}
