package no.nav.veilarboppfolging.db;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.NyeBrukereFeedDTO;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.veilarboppfolging.domain.SykmeldtBrukerType;
import no.nav.sbl.jdbc.Database;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

@Slf4j
@Repository
public class NyeBrukereFeedRepository {

    private final Database database;

    @Inject
    public NyeBrukereFeedRepository(Database database) {
        this.database = database;
    }


    public void leggTil(Oppfolgingsbruker oppfolgingsbruker) {
        Innsatsgruppe innsatsgruppe = oppfolgingsbruker.getInnsatsgruppe();
        SykmeldtBrukerType sykmeldtBrukerType = oppfolgingsbruker.getSykmeldtBrukerType();
        String innsatsGruppeNavn = innsatsgruppe == null ? null : innsatsgruppe.toString();
        String sykmeldtBrukerTypeNavn = sykmeldtBrukerType == null ? null : sykmeldtBrukerType.toString();
        database.update(
                "INSERT INTO NYE_BRUKERE_FEED " +
                "(AKTOR_ID, FORESLATT_INNSATSGRUPPE, SYKMELDTBRUKERTYPE) " +
                "VALUES" +
                "(?,?,?)", oppfolgingsbruker.getAktoerId(), innsatsGruppeNavn, sykmeldtBrukerTypeNavn);
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
                .foreslattInnsatsgruppe(rs.getString("FORESLATT_INNSATSGRUPPE"))
                .sykmeldtBrukerType(rs.getString("SYKMELDTBRUKERTYPE"))
                .opprettet(rs.getTimestamp("OPPRETTET_TIMESTAMP"))
                .build();
    }
}
