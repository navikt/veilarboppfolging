package no.nav.fo.veilarboppfolging.db;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.Optional;

public class ArbeidssokerregistreringRepository {

    private JdbcTemplate db;

    private final static String BRUKER_REGISTRERING_SEQ = "BRUKER_REGISTRERING_SEQ";
    private final static String BRUKER_REGISTRERING = "BRUKER_REGISTRERING";
    private final static String BRUKER_REGISTRERING_ID = "BRUKER_REGISTRERING_ID";
    private final static String OPPRETTET_DATO = "OPPRETTET_DATO";
    private final static String ENIG_I_OPPSUMMERING = "ENIG_I_OPPSUMMERING";
    private final static String OPPSUMMERING = "OPPSUMMERING";
    private final static String BESVARELSE = "BESVARELSE";

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

    @Transactional
    public BrukerRegistrering registrerBruker(BrukerRegistrering bruker) {
        long id = nesteFraSekvens(BRUKER_REGISTRERING_SEQ);
        SqlUtils.insert(db, BRUKER_REGISTRERING)
                .value(BRUKER_REGISTRERING_ID, id)
                .value(AKTOR_ID, bruker.getAktorId())
                .value(OPPRETTET_DATO, bruker.getOpprettetDato())
                .value(ENIG_I_OPPSUMMERING, bruker.isEnigIOppsummering())
                .value(OPPSUMMERING, bruker.getOppsummering())
                .value(BESVARELSE, bruker.getBesvarelse())
                .execute();

        return SqlUtils.select(db.getDataSource(), BRUKER_REGISTRERING, ArbeidssokerregistreringRepository::brukerRegistreringMapper)
                .column(ENIG_I_OPPSUMMERING)
                .column(OPPSUMMERING)
                .column(BESVARELSE)
                .where(WhereClause.equals(AKTOR_ID, bruker.getAktorId()))
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
    private static BrukerRegistrering brukerRegistreringMapper(ResultSet rs) {
        return new BrukerRegistrering()
                .setEnigIOppsummering(rs.getBoolean(ENIG_I_OPPSUMMERING))
                .setOppsummering(rs.getString(OPPSUMMERING))
                .setBesvarelse(rs.getString(BESVARELSE));
    }
}
