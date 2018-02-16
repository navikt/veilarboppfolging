package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.RegistrertBruker;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.Optional;

public class ArbeidssokerregistreringRepository {

    private JdbcTemplate db;

    private final static String BRUKER_REGISTRERING_SEQ = "BRUKER_REGISTRERING_SEQ";
    private final static String BRUKER_REGISTRERING = "BRUKER_REGISTRERING";
    private final static String BRUKER_REGISTRERING_ID = "BRUKER_REGISTRERING_ID";
    private final static String OPPRETTET_DATO = "OPPRETTET_DATO";

    private final static String NUS_KODE = "NUS_KODE";
    private final static String YRKESPRAKSIS = "YRKESPRAKSIS";
    private final static String ENIG_I_OPPSUMMERING = "ENIG_I_OPPSUMMERING";
    private final static String OPPSUMMERING = "OPPSUMMERING";
    private final static String UTDANNING_BESTATT = "UTDANNING_BESTATT";
    private final static String UTDANNING_GODKJENT_NORGE = "UTDANNING_GODKJENT_NORGE";
    private final static String HAR_HELSEUTFORDRINGER = "HAR_HELSEUTFORDRINGER";
    private final static String SITUASJON = "SITUASJON";

    private final static String OPPFOLGINGSTATUS = "OPPFOLGINGSTATUS";
    private final static String UNDER_OPPFOLGING = "UNDER_OPPFOLGING";
    private final static String AKTOR_ID = "AKTOR_ID";

    public ArbeidssokerregistreringRepository(JdbcTemplate db) {
        this.db = db;
    }

    public boolean erOppfolgingsflaggSatt(AktorId aktorid) {
        return Optional.ofNullable(SqlUtils.select(db.getDataSource(), OPPFOLGINGSTATUS, ArbeidssokerregistreringRepository::oppfolgignsflaggMapper)
                .column(UNDER_OPPFOLGING)
                .where(WhereClause.equals(AKTOR_ID, aktorid.getAktorId()))
                .execute()).orElse(false);
    }

    public RegistrertBruker lagreBruker(RegistrertBruker bruker, AktorId aktorId) {
        long id = nesteFraSekvens(BRUKER_REGISTRERING_SEQ);
        SqlUtils.insert(db, BRUKER_REGISTRERING)
                .value(BRUKER_REGISTRERING_ID, id)
                .value(AKTOR_ID, aktorId.getAktorId())
                .value(OPPRETTET_DATO, DbConstants.CURRENT_TIMESTAMP)
                .value(NUS_KODE, bruker.getNusKode())
                .value(YRKESPRAKSIS, bruker.getYrkesPraksis())
                .value(ENIG_I_OPPSUMMERING, bruker.isEnigIOppsummering())
                .value(OPPSUMMERING, bruker.getOppsummering())
                .value(UTDANNING_BESTATT, bruker.isUtdanningBestatt())
                .value(UTDANNING_GODKJENT_NORGE, bruker.isUtdanningGodkjentNorge())
                .value(HAR_HELSEUTFORDRINGER, bruker.isHarHelseutfordringer())
                .value(SITUASJON, bruker.getSituasjon())
                .execute();

        return SqlUtils.select(db.getDataSource(), BRUKER_REGISTRERING, ArbeidssokerregistreringRepository::brukerRegistreringMapper)
                .where(WhereClause.equals(AKTOR_ID, aktorId.getAktorId()))
                .column("*")
                .execute();
    }

    private long nesteFraSekvens(String sekvensNavn) {
        return ((Long)this.db.queryForObject("select " + sekvensNavn + ".nextval from dual", Long.class)).longValue();
    }

    @SneakyThrows
    private static boolean oppfolgignsflaggMapper(ResultSet rs) {
        return rs.getBoolean(UNDER_OPPFOLGING);
    }

    @SneakyThrows
    private static RegistrertBruker brukerRegistreringMapper(ResultSet rs) {
        return new RegistrertBruker(
                rs.getString(NUS_KODE),
                rs.getString(YRKESPRAKSIS),
                rs.getDate(OPPRETTET_DATO),
                rs.getBoolean(ENIG_I_OPPSUMMERING),
                rs.getString(OPPSUMMERING),
                rs.getBoolean(UTDANNING_BESTATT),
                rs.getBoolean(UTDANNING_GODKJENT_NORGE),
                rs.getBoolean(HAR_HELSEUTFORDRINGER),
                rs.getString(SITUASJON));
    }
}
