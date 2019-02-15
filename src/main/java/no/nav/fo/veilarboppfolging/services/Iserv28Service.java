package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsStatusRepository;
import no.nav.fo.veilarboppfolging.domain.IservMapper;
import no.nav.fo.veilarboppfolging.domain.OppfolgingTable;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.metrics.utils.MetricsUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.common.auth.SubjectHandler.withSubject;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erIserv;
import static no.nav.fo.veilarboppfolging.services.ArenaUtils.erUnderOppfolging;
import static no.nav.fo.veilarboppfolging.services.Iserv28Service.AvslutteOppfolgingResultat.*;
import static no.nav.sbl.sql.DbConstants.CURRENT_TIMESTAMP;

@Component
@Slf4j
public class Iserv28Service{

    enum AvslutteOppfolgingResultat {
        AVSLUTTET_OK,
        IKKE_AVSLUTTET,
        IKKE_LENGER_UNDER_OPPFØLGING,
        AVSLUTTET_FEILET
    }

    private final JdbcTemplate jdbc;
    private final OppfolgingService oppfolgingService;
    private final AktorService aktorService;
    private final LockingTaskExecutor taskExecutor;
    private final SystemUserSubjectProvider systemUserSubjectProvider;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final OppfolgingRepository oppfolgingRepository;
    private final UnleashService unleashService;

    private static final int lockAutomatiskAvslutteOppfolgingSeconds = 3600;

    @Inject
    public Iserv28Service(
            JdbcTemplate jdbc,
            OppfolgingService oppfolgingService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            OppfolgingRepository oppfolgingRepository,
            AktorService aktorService,
            LockingTaskExecutor taskExecutor,
            SystemUserSubjectProvider systemUserSubjectProvider,
            UnleashService unleashService
    ){
        this.jdbc = jdbc;
        this.oppfolgingService = oppfolgingService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.aktorService = aktorService;
        this.taskExecutor = taskExecutor;
        this.systemUserSubjectProvider = systemUserSubjectProvider;
        this.unleashService = unleashService;
    }

    @Scheduled(cron="0 0 * * * *")
    public void scheduledAvslutteOppfolging() {
        Instant lockAtMostUntil = Instant.now().plusSeconds(lockAutomatiskAvslutteOppfolgingSeconds);
        Instant lockAtLeastUntil = Instant.now().plusSeconds(10);
        taskExecutor.executeWithLock(
                this::automatiskAvslutteOppfolging,
                new LockConfiguration("oppdaterAvlutteOppfolging", lockAtMostUntil, lockAtLeastUntil)
        );
    }

    void automatiskAvslutteOppfolging() {

        MetricsUtils.timed("oppfolging.automatisk.avslutning", () ->   {
            long start = System.currentTimeMillis();
            List<AvslutteOppfolgingResultat> resultater = finnBrukereOgAvslutt();
            log.info("Avslutter jobb for automatisk avslutning av brukere. Tid brukt: {} ms. Antall [Avsluttet/Ikke avsluttet/Ikke lenger under oppfølging/Feilet/Totalt]: [{}/{}/{}/{}/{}]", 
                System.currentTimeMillis() - start,
                resultater.stream().filter(r -> r == AVSLUTTET_OK).count(),
                resultater.stream().filter(r -> r == IKKE_AVSLUTTET).count(),
                resultater.stream().filter(r -> r == IKKE_LENGER_UNDER_OPPFØLGING).count(),
                resultater.stream().filter(r -> r == AVSLUTTET_FEILET).count(),
                resultater.size());
        });
    }

    private List<AvslutteOppfolgingResultat> finnBrukereOgAvslutt() {
        List<AvslutteOppfolgingResultat> resultater = new ArrayList<>();
        try {
            log.info("Starter jobb for automatisk avslutning av brukere");
            List<IservMapper> iservert28DagerBrukere = finnBrukereMedIservI28Dager();
            log.info("Fant {} brukere som har vært ISERV mer enn 28 dager", iservert28DagerBrukere.size());
            withSubject(systemUserSubjectProvider.getSystemUserSubject(), () -> {
                resultater.addAll(iservert28DagerBrukere.stream()
                        .map(iservMapper -> avslutteOppfolging(iservMapper.aktor_Id))
                        .collect(toList()));
            });
        } catch (Exception e) {
            log.error("Feil ved automatisk avslutning av brukere", e);
        }
        return resultater;
    }

    public void filterereIservBrukere(ArenaBruker arenaBruker){

        try {
            IservMapper eksisterendeIservBruker = eksisterendeIservBruker(arenaBruker);
            String aktoerid = arenaBruker.getAktoerid();

            if(erIserv(arenaBruker.getFormidlingsgruppekode())) {
                if (eksisterendeIservBruker != null) {
                    updateIservBruker(arenaBruker);
                } else if (brukerHarOppfolgingsflagg(aktoerid)) {
                    insertIservBruker(arenaBruker);
                }
            } else {
                if(eksisterendeIservBruker != null) {
                    slettAvsluttetOppfolgingsBruker(aktoerid);
                }
                if(erUnderOppfolging(arenaBruker.getFormidlingsgruppekode(), arenaBruker.getKvalifiseringsgruppekode(), null) 
                        && !brukerHarOppfolgingsflagg(aktoerid)) {
                    if(unleashService.isEnabled("veilarboppfolging.start.oppfolging.automatisk")) {
                        log.info("Starter oppfølging automatisk for bruker med aktørid{}", aktoerid);
                        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktoerid);
                    } else {
                        log.info("Automatisk start av oppfølging er slått av i unleash. Aktørid {}", aktoerid);
                    }
                }
            }
        }
        catch(Exception e){
            log.error("Exception ved filterereIservBrukere: {}" , e);
        }
    }

    private boolean brukerHarOppfolgingsflagg(String aktoerId) {
        OppfolgingTable eksisterendeOppfolgingstatus = oppfolgingsStatusRepository.fetch(aktoerId);
        boolean harOppfolgingsflagg = eksisterendeOppfolgingstatus != null && eksisterendeOppfolgingstatus.isUnderOppfolging();
        log.info("Bruker med aktorid {} har oppfolgingsflagg: {}", aktoerId, harOppfolgingsflagg);
        return harOppfolgingsflagg;
    }

    public IservMapper eksisterendeIservBruker(ArenaBruker arenaBruker){
         return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("iserv_fra_dato")
                .where(WhereClause.equals("aktor_id",arenaBruker.getAktoerid())).execute();
    }

    private void updateIservBruker(ArenaBruker arenaBruker){
        SqlUtils.update(jdbc, "UTMELDING")
                .set("iserv_fra_dato", Timestamp.from(arenaBruker.getIserv_fra_dato().toInstant()))
                .set("oppdatert_dato", CURRENT_TIMESTAMP)
                .whereEquals("aktor_id", arenaBruker.getAktoerid())
                .execute();

        log.info("ISERV bruker med aktorid {} har blitt oppdatert inn i UTMELDING tabell", arenaBruker.getAktoerid());
    }

    void insertIservBruker(ArenaBruker arenaBruker) {
        Timestamp iservFraDato = Timestamp.from(arenaBruker.getIserv_fra_dato().toInstant());
        SqlUtils.insert(jdbc, "UTMELDING")
                .value("aktor_id", arenaBruker.getAktoerid())
                .value("iserv_fra_dato", iservFraDato)
                .value("oppdatert_dato", CURRENT_TIMESTAMP)
                .execute();

        log.info("ISERV bruker med aktorid {} og iserv_fra_dato {} har blitt insertert inn i UTMELDING tabell",
                arenaBruker.getAktoerid(),
                iservFraDato
        );
    }
    
    AvslutteOppfolgingResultat avslutteOppfolging(String aktoerId) {
        AvslutteOppfolgingResultat resultat;
        try {
            if(!brukerHarOppfolgingsflagg(aktoerId)) {
                log.info("Bruker med aktørid {} har ikke oppfølgingsflagg. Sletter fra utmelding-tabell", aktoerId);
                slettAvsluttetOppfolgingsBruker(aktoerId);
                resultat = IKKE_LENGER_UNDER_OPPFØLGING;
            } else {
                String fnr = aktorService.getFnr(aktoerId).orElseThrow(IllegalStateException::new);
                boolean oppfolgingAvsluttet = oppfolgingService.avsluttOppfolgingForSystemBruker(
                        fnr,
                        "System",
                        "Oppfolging avsluttet autmatisk for grunn av iservert 28 dager"
                );
                resultat = oppfolgingAvsluttet ? AVSLUTTET_OK : IKKE_AVSLUTTET;
                if(oppfolgingAvsluttet) {
                    slettAvsluttetOppfolgingsBruker(aktoerId);
                    FunksjonelleMetrikker.antallBrukereAvsluttetAutomatisk();
                }
            }
        } catch (Exception e) {
            log.error("Automatisk avsluttOppfolging feilet for aktoerid {} ", aktoerId, e);
            resultat = AVSLUTTET_FEILET;
        }
        return resultat;
    }

    private void slettAvsluttetOppfolgingsBruker(String aktoerId) {
        WhereClause aktoeridClause = WhereClause.equals("aktor_id", aktoerId);
        SqlUtils.delete(jdbc, "UTMELDING").where(aktoeridClause).execute();
        log.info("Aktorid {} har blitt slettet fra UTMELDING tabell", aktoerId);
    }

    public List<IservMapper> finnBrukereMedIservI28Dager() {
        Timestamp tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28));
        WhereClause harAktoerId = WhereClause.isNotNull("aktor_id");
        WhereClause iservDato28DagerTilbake = WhereClause.lt("iserv_fra_dato", tilbake28);

        return SqlUtils.select(jdbc, "UTMELDING", Iserv28Service::mapper)
                .column("aktor_id")
                .column("iserv_fra_dato")
                .where(harAktoerId.and(iservDato28DagerTilbake))
                .executeToList();
    }

    private static IservMapper mapper(ResultSet resultSet) throws SQLException {
        return new IservMapper(
                resultSet.getString("aktor_id"),
                resultSet.getTimestamp("iserv_fra_dato").toLocalDateTime().atZone(ZoneId.systemDefault())
        );
    }

}
