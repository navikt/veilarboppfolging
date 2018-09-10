package no.nav.fo.veilarboppfolging.services;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.Iserv28;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.utils.DateUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
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
        try {
            List<Iserv28> iservert28DagerBrukerne = finnBrukereMedIservI28Dager();
            iservert28DagerBrukerne.stream().forEach(iservBruker -> avsluttOppfolging(iservBruker.aktor_Id));
        } catch (Exception e) {
            log.error("Feil ved automatisk avslutning av brukere", e);
        }
    }

    public void filterereIservBrukere(ArenaBruker arenaBruker){
        if(eksisterendeIservBruker(arenaBruker) != null && !arenaBruker.formidlingsgruppekode.equals("ISERV")) {
            slettAvluttetOppfolgingsBruker(arenaBruker.getAktoerid());
        }else if(arenaBruker.getFormidlingsgruppekode().equals("ISERV")){
            insertIservBruker(
                    arenaBruker.aktoerid,
                    arenaBruker.formidlingsgruppekode,
                    DateUtils.toTimeStamp(arenaBruker.iserv_fra_dato)
            );
        }
    }

    private Iserv28 eksisterendeIservBruker(ArenaBruker arenaBruker){
         return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("formidlingsgruppekode")
                .where(WhereClause.equals("aktor_id",arenaBruker.getAktoerid())).execute();
    }

    private void insertIservBruker(String aktoerId, String formidlingsgruppekode, Timestamp iserv_fra_dato) {
        SqlUtils.insert(jdbc, "UTMELDING")
                .value("aktor_id", aktoerId)
                .value("formidlingsgruppekode", formidlingsgruppekode)
                .value("iserv_fra_dato", iserv_fra_dato)
                .execute();
    }

    private void avsluttOppfolging(String aktoerId) {
        String fnr = aktorService.getFnr(aktoerId).orElseThrow(() -> new IllegalArgumentException("Fant ikke fnr for aktoerid: " + aktoerId));
        oppfolgingService.avsluttOppfolging(
                 fnr,
                "System",
                "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
        );

        slettAvluttetOppfolgingsBruker(aktoerId);
    }

    private void slettAvluttetOppfolgingsBruker(String aktoerId) {
        WhereClause aktoerid = WhereClause.equals("aktor_id", aktoerId);
        SqlUtils.delete(jdbc, "UTMELDING").where(aktoerid).execute();
    }

    private List<Iserv28> finnBrukereMedIservI28Dager() {
        Timestamp tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28));
        WhereClause erIserv = WhereClause.equals("formidlingsgruppekode", "ISERV");
        WhereClause harAktoerId = WhereClause.isNotNull("aktoerid");
        WhereClause iservDato28DagerTilbake = WhereClause.lteq("iserv_fra_dato", tilbake28);

        return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("formidlingsgruppekode")
                .column("iserv_fra_dato")
                .where(erIserv.and(harAktoerId).and(iservDato28DagerTilbake))
                .executeToList();
    }

    private static Iserv28 mapper(ResultSet resultSet) throws SQLException {
        return new Iserv28(
                resultSet.getString("aktor_id"),
                resultSet.getString("formidlingsgruppekode"),
                resultSet.getTimestamp("iserv_fra_dato").toLocalDateTime().atZone(ZoneId.systemDefault())
        );
    }

}
