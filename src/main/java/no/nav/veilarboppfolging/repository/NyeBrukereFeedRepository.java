package no.nav.veilarboppfolging.repository;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.veilarboppfolging.feed.domain.NyeBrukereFeedDTO;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

@Slf4j
@Repository
public class NyeBrukereFeedRepository {

    private final JdbcTemplate db;

    @Autowired
    public NyeBrukereFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void leggTil(Oppfolgingsbruker oppfolgingsbruker) {
        Innsatsgruppe innsatsgruppe = oppfolgingsbruker.getInnsatsgruppe();
        SykmeldtBrukerType sykmeldtBrukerType = oppfolgingsbruker.getSykmeldtBrukerType();
        String innsatsGruppeNavn = innsatsgruppe == null ? null : innsatsgruppe.toString();
        String sykmeldtBrukerTypeNavn = sykmeldtBrukerType == null ? null : sykmeldtBrukerType.toString();
        db.update(
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
        return db.update(
                "UPDATE NYE_BRUKERE_FEED " +
                    "SET FEED_ID = NYE_BRUKERE_FEED_SEQ.NEXTVAL " +
                    "WHERE FEED_ID IS NULL");
    }

    public List<NyeBrukereFeedDTO> hentElementerStorreEnnId(String id, int pageSize) {
        return db.query(
                "SELECT * FROM " +
                    "(SELECT * FROM NYE_BRUKERE_FEED WHERE FEED_ID > ? ORDER BY FEED_ID) " +
                    "WHERE ROWNUM <= ?", NyeBrukereFeedRepository::nyBrukerMapper, id, pageSize);
    }

    @SneakyThrows
    private static NyeBrukereFeedDTO nyBrukerMapper(ResultSet rs, int row) {
        return NyeBrukereFeedDTO.builder()
                .id(rs.getLong("FEED_ID"))
                .aktorId(rs.getString("AKTOR_ID"))
                .foreslattInnsatsgruppe(rs.getString("FORESLATT_INNSATSGRUPPE"))
                .sykmeldtBrukerType(rs.getString("SYKMELDTBRUKERTYPE"))
                .opprettet(DbUtils.hentZonedDateTime(rs, "OPPRETTET_TIMESTAMP"))
                .build();
    }
}
