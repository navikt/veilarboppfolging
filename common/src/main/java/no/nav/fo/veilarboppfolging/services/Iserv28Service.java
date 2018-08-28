package no.nav.fo.veilarboppfolging.services;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.Iserv28;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.utils.DateUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class Iserv28Service{

    private JdbcTemplate jdbc;

    @Inject
    private OppfolgingService oppfolgingService;

    @Inject
    private AktorService aktorService;

    @Inject
    public Iserv28Service(JdbcTemplate jdbc){
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "* * * 10")
    public void automatiskAvlutteOppfolging() {
        List<Iserv28> iservert28DagerBrukerne = finnBrukereMedIservI28Dager();
        iservert28DagerBrukerne.stream().forEach(iservBruker -> avsluttOppfolging(iservBruker.aktoerId));
    }

    public void filterereIservBrukere(ArenaBruker iservMapper){
        if(iservMapper.formidlingsgruppekode.equals("ISERV")) {
            insertIservBruker(
                    iservMapper.aktoerid,
                    iservMapper.formidlingsgruppekode,
                    DateUtils.toTimeStamp(iservMapper.iserv_fra_dato)
            );
        }
    }

    public void insertIservBruker(String aktoerId, String formidlingsgruppekode, Timestamp iserv_fra_dato) {
        SqlUtils.insert(jdbc, "UTMELDING")
                .value("aktor_id", aktoerId)
                .value("formidlingsgruppekode", formidlingsgruppekode)
                .value("iserv_fra_dato", iserv_fra_dato)
                .execute();
    }

    public void avsluttOppfolging(String aktoerId) {
        String fnr = aktorService.getFnr(aktoerId).orElseThrow(() -> new IllegalArgumentException("Fant ikke fnr for aktoerid: " + aktoerId));
        oppfolgingService.avsluttOppfolging(
                 fnr,
                "System",
                "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
        );

        slettAvluttetOppfolgingsBruker(aktoerId);
    }

    public void slettAvluttetOppfolgingsBruker(String aktoerId) {
        WhereClause aktoerid = WhereClause.equals("aktoerid", aktoerId);
        SqlUtils.delete(jdbc, "utmelding").where(aktoerid).execute();
    }

    public List<Iserv28> finnBrukereMedIservI28Dager() {
        Timestamp tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28));
        WhereClause erIserv = WhereClause.equals("formidlingsgruppekode", "ISERV");
        WhereClause harAktoerId = WhereClause.isNotNull("aktoerid");
        WhereClause iservDato28DagerTilbake = WhereClause.lteq("iserv_fra_dato", tilbake28);

        return SqlUtils.select(jdbc, "utmelding", Iserv28Service::mapper)
                .column("aktoerid")
                .column("formidlingsgruppekode")
                .column("iserv_fra_dato")
                .where(erIserv.and(harAktoerId).and(iservDato28DagerTilbake))
                .executeToList();
    }

    private static Iserv28 mapper(ResultSet resultSet) throws SQLException {
        return new Iserv28(
                resultSet.getString("aktoerid"),
                resultSet.getString("formidlingsgruppekode"),
                resultSet.getTimestamp("iserv_fra_dato").toLocalDateTime().atZone(ZoneId.systemDefault())
        );
    }

}
