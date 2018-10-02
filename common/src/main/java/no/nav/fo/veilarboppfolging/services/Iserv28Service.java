package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.domain.Iserv28;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.*;
import java.time.*;
import java.util.List;

import static java.util.Optional.of;
import static no.nav.sbl.sql.DbConstants.CURRENT_TIMESTAMP;

@Component
@Slf4j
public class Iserv28Service{

    private final JdbcTemplate jdbc;
    private final OppfolgingService oppfolgingService;
    private final AktorService aktorService;

    @Inject
    private LockingTaskExecutor taskExecutor;

    @Value("${lock.automatiskAvslutteOppfolging.seconds:3600}")
    private int lockAutomatiskAvslutteOppfolgingSeconds;

    @Inject
    public Iserv28Service(JdbcTemplate jdbc, OppfolgingService oppfolgingService, AktorService aktorService){
        this.jdbc = jdbc;
        this.oppfolgingService = oppfolgingService;
        this.aktorService = aktorService;
    }

    @Scheduled(fixedDelay = 10000L, initialDelay = 1000L)
    public void scheduledAvlutteOppfolging() {
        Instant lockAtMostUntil = Instant.now().plusSeconds(lockAutomatiskAvslutteOppfolgingSeconds);
        Instant lockAtLeastUntil = Instant.now().plusSeconds(10);
        taskExecutor.executeWithLock(
                () -> automatiskAvlutteOppfolging(),
                new LockConfiguration("oppdaterAvlutteOppfolging", lockAtMostUntil, lockAtLeastUntil)
        );
    }

    public void automatiskAvlutteOppfolging() {
        try {
            List<Iserv28> iservert28DagerBrukerne = finnBrukereMedIservI28Dager();
            iservert28DagerBrukerne.stream().forEach(iservBruker -> avsluttOppfolging(iservBruker.aktor_Id));
        } catch(Exception e) {
            log.error("Feil ved automatisk avslutning av brukere", e);
        }
    }

    public void filterereIservBrukere(ArenaBruker arenaBruker){
        if (eksisterendeIservBruker(arenaBruker)!= null && !arenaBruker.formidlingsgruppekode.equals("ISERV")) {
            slettAvluttetOppfolgingsBruker(arenaBruker.getAktoerid());
        } else if(eksisterendeIservBruker(arenaBruker)!= null){
            updateIservBruker();
        } else if(arenaBruker.getFormidlingsgruppekode().equals("ISERV")){
            insertIservBruker(
                    arenaBruker.aktoerid,
                    of(arenaBruker.iserv_fra_dato).map(ZonedDateTime::toInstant).map(Timestamp::from).orElseThrow(IllegalStateException::new)
            );
        }
    }

    public Iserv28 eksisterendeIservBruker(ArenaBruker arenaBruker){
         return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("iserv_fra_dato")
                .where(WhereClause.equals("aktor_id",arenaBruker.getAktoerid())).execute();
    }

    public void updateIservBruker(){
        SqlUtils.update(jdbc, "UTMELDING")
                .set("oppdatert_dato", CURRENT_TIMESTAMP)
                .execute();
    }

    public void insertIservBruker(String aktoerId, Timestamp iserv_fra_dato) {
        SqlUtils.insert(jdbc, "UTMELDING")
                .value("aktor_id", aktoerId)
                .value("iserv_fra_dato", iserv_fra_dato)
                .value("oppdatert_dato", CURRENT_TIMESTAMP)
                .execute();
    }

    public void avsluttOppfolging(String aktoerId) {
        try {
            String fnr = aktorService.getFnr(aktoerId).toString();
            oppfolgingService.avsluttOppfolging(
                    fnr,
                    "System",
                    "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
            );
            slettAvluttetOppfolgingsBruker(aktoerId);
        } catch(IllegalArgumentException e){
            log.error("Automatisk avsluttOppfolging feilet for aktoerid:"+ aktoerId, e);
        }
    }

    public void slettAvluttetOppfolgingsBruker(String aktoerId) {
        WhereClause aktoerid = WhereClause.equals("aktor_id", aktoerId);
        SqlUtils.delete(jdbc, "UTMELDING").where(aktoerid).execute();
    }

    public List<Iserv28> finnBrukereMedIservI28Dager() {
        Timestamp tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28));
        WhereClause harAktoerId = WhereClause.isNotNull("aktor_id");
        WhereClause iservDato28DagerTilbake = WhereClause.lt("iserv_fra_dato", tilbake28);

        return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("iserv_fra_dato")
                .where(harAktoerId.and(iservDato28DagerTilbake))
                .executeToList();
    }

    private static Iserv28 mapper(ResultSet resultSet) throws SQLException {
        return new Iserv28(
                resultSet.getString("aktor_id"),
                resultSet.getTimestamp("iserv_fra_dato").toLocalDateTime().atZone(ZoneId.systemDefault())
        );
    }

}
