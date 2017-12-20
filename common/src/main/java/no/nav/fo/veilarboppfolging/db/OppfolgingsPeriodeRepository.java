package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.AvsluttetOppfolgingFeedData;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsperiode;
import no.nav.sbl.jdbc.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static no.nav.sbl.jdbc.Database.hentDato;

public class OppfolgingsPeriodeRepository {
    private Database database;

    public OppfolgingsPeriodeRepository(Database database) {
        this.database = database;
    }

    public void opprettOppfolgingsperiode(String aktorId) {
        database.update("" +
                        "INSERT INTO OPPFOLGINGSPERIODE(aktor_id, startDato, oppdatert) " +
                        "VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                aktorId);
    }

    public void avsluttOppfolgingsperiode(String aktorId, String veileder, String begrunnelse) {
        database.update("" +
                        "UPDATE OPPFOLGINGSPERIODE " +
                        "SET avslutt_veileder = ?, " +
                        "avslutt_begrunnelse = ?, " +
                        "sluttDato = CURRENT_TIMESTAMP, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ? " +
                        "AND sluttDato IS NULL",
                veileder,
                begrunnelse,
                aktorId);
    }

    public List<AvsluttetOppfolgingFeedData> hentAvsluttetOppfolgingEtterDato(Timestamp timestamp, int pageSize) {
        return database
                .query("SELECT * FROM (SELECT aktor_id, sluttdato, oppdatert " +
                                "FROM OPPFOLGINGSPERIODE " +
                                "WHERE oppdatert >= ? and sluttdato is not null order by oppdatert) " +
                                "WHERE rownum <= ?",
                        this::mapRadTilAvsluttetOppfolging,
                        timestamp,
                        pageSize);
    }



    public List<Oppfolgingsperiode> hentOppfolgingsperioder(String aktorId) {
        return database.query(hentOppfolingsperioderSQL +
                        "WHERE aktor_id = ?",
                this::mapTilOppfolgingsperiode,
                aktorId
        );
    }

    public List<Oppfolgingsperiode> hentAvsluttetOppfolgingsperioder(String aktorId) {
        return database.query(hentOppfolingsperioderSQL +
                        "WHERE aktor_id = ? AND sluttdato is not null",
                this::mapTilOppfolgingsperiode,
                aktorId
        );
    }

    private final static String hentOppfolingsperioderSQL =
            "SELECT aktor_id, avslutt_veileder, startdato, sluttdato, avslutt_begrunnelse " +
                    "FROM OPPFOLGINGSPERIODE ";

    private Oppfolgingsperiode mapTilOppfolgingsperiode(ResultSet result) throws SQLException {
        return Oppfolgingsperiode.builder()
                .aktorId(result.getString("aktor_id"))
                .veileder(result.getString("avslutt_veileder"))
                .startDato(hentDato(result, "startdato"))
                .sluttDato(hentDato(result, "sluttdato"))
                .begrunnelse(result.getString("avslutt_begrunnelse"))
                .build();
    }

    @SneakyThrows
    private AvsluttetOppfolgingFeedData mapRadTilAvsluttetOppfolging(ResultSet rs) {
        return AvsluttetOppfolgingFeedData.builder()
                .aktoerid(rs.getString("aktor_id"))
                .sluttdato(rs.getTimestamp("sluttdato"))
                .oppdatert(rs.getTimestamp("oppdatert"))
                .build();
    }

}
