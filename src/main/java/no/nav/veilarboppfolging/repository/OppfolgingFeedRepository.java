package no.nav.veilarboppfolging.repository;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.veilarboppfolging.feed.domain.OppfolgingFeedDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Repository
public class OppfolgingFeedRepository {

    private final JdbcTemplate db;
    @Autowired
    public OppfolgingFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<Long> hentAntallBrukere() {
        val sql = "SELECT "
                + "count(*) "
                + "from "
                + "OPPFOLGINGSTATUS os LEFT JOIN MANUELL_STATUS ms "
                + "on (os.GJELDENDE_MANUELL_STATUS = ms.ID) "
                + ", "
                + "(select "
                + "AKTOR_ID, "
                + "STARTDATO "
                + "from OPPFOLGINGSPERIODE "
                + "where SLUTTDATO is null "
                + "  ) siste_periode "
                + "where os.AKTOR_ID = siste_periode.AKTOR_ID";

        Long count = db.query(sql, rs -> {
            rs.next();
            return rs.getLong(1);
        });
        return Optional.ofNullable(count);
    }

    public List<OppfolgingFeedDTO> hentEndringerEtterId(String sinceId, int pageSize) {

        // 1. Join sammen Tabellen OPPFOLGINGSPERIODE og OPPFOLGINGSTATUS på AKTOR_ID
        // 2. LEFT JOIN inn tabellen MANUELL_STATUS siden ikke alle brukere ligger i denne tabellen (de radene hvor det ikke er noe match på id vil få null i MANUELL-kolonnen)
        // 3. Det er nå flere duplikate rader siden én bruker kan ha flere startdatoer, vi må filtrere ut de radene for en bruker som har den nyeste startdatoen,
        //    partition_by grupperer på AKTOR_ID for så å sortere på startdato hvor den nyeste ligger øverst, first_value velger så den øverste raden.

        return db.queryForList(
        "SELECT DISTINCT "
                + "os.AKTOR_ID,"
                + "os.VEILEDER,"
                + "os.UNDER_OPPFOLGING,"
                + "os.NY_FOR_VEILEDER,"
                + "os.OPPDATERT,"
                + "os.FEED_ID,"
                + "m.MANUELL,"
                // grupper duplikate rader og sorter på startdato, velg så den øverste raden (som da er den med nyeste startdatoen)
                // https://stackoverflow.com/questions/10515391/oracle-equivalent-of-postgres-distinct-on
                + "first_value(op.STARTDATO) over (partition BY os.AKTOR_ID ORDER BY op.STARTDATO DESC) AS STARTDATO "
                + "FROM "
                + "OPPFOLGINGSPERIODE op,"
                // LEFT JOIN fordi ikke alle brukere ligger i MANUELL_STATUS
                // (https://www.oracletutorial.com/oracle-basics/oracle-joins/)
                + "OPPFOLGINGSTATUS os LEFT JOIN MANUELL_STATUS m ON (os.GJELDENDE_MANUELL_STATUS = m.ID) "
                + "WHERE os.AKTOR_ID = op.AKTOR_ID "
                + "AND os.FEED_ID >= ? "
                + "AND ROWNUM <= ? "
                + "ORDER BY os.FEED_ID",
                sinceId,
                pageSize
        ).stream()
                .map(OppfolgingFeedRepository::mapRadTilOppfolgingFeedDTO)
                .collect(toList());
    }

    public void insertFeedId() {
        long start = System.currentTimeMillis();
        int updatedRows = db.update(
                "UPDATE OPPFOLGINGSTATUS " +
                        "SET FEED_ID = OPPFOLGING_FEED_SEQ.NEXTVAL " +
                        "WHERE FEED_ID IS NULL");
        if (updatedRows > 0) {
            log.info("Satte feed-id på {} rader. Tid brukt: {} ms", updatedRows, System.currentTimeMillis() - start);
        }
    }

    public static OppfolgingFeedDTO mapRadTilOppfolgingFeedDTO(Map<String, Object> rad) {
        return OppfolgingFeedDTO
                .builder()
                .aktoerid((String) rad.get("AKTOR_ID"))
                .veileder((String) rad.get("VEILEDER"))
                .oppfolging(rad.get("UNDER_OPPFOLGING").equals(BigDecimal.ONE))
                .nyForVeileder(rad.get("NY_FOR_VEILEDER").equals(BigDecimal.ONE))
                .endretTimestamp((Timestamp) (rad.get("OPPDATERT")))
                .startDato((Timestamp) (rad.get("STARTDATO")))
                .feedId((BigDecimal) rad.get("FEED_ID"))
                .manuell(BigDecimal.ONE.equals(rad.get("MANUELL")))
                .build();
    }
}
