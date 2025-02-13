package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.domain.StartetAvType;
import no.nav.veilarboppfolging.oppfolgingsbruker.OppfolgingStartBegrunnelse;
import no.nav.veilarboppfolging.oppfolgingsbruker.Oppfolgingsbruker;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.utils.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.AKTOR_ID;
import static no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.UNDER_OPPFOLGING;
import static no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime;
import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;

@Repository
public class OppfolgingsPeriodeRepository {

    private final JdbcTemplate db;

    private final TransactionTemplate transactor;

    private final static String hentOppfolingsperioderSQL =
            "SELECT uuid, aktor_id, avslutt_veileder, startdato, sluttdato, avslutt_begrunnelse, start_begrunnelse " +
                    "FROM OPPFOLGINGSPERIODE ";

    @Autowired
    public OppfolgingsPeriodeRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
        this.transactor = transactor;
    }

    public void start(Oppfolgingsbruker oppfolgingsbruker) {
        transactor.executeWithoutResult((ignored) -> {
            insert(oppfolgingsbruker.getAktorId(), oppfolgingsbruker.getOppfolgingStartBegrunnelse(), oppfolgingsbruker.getRegistrertAv(), oppfolgingsbruker.getStartetAvType());
            setActive(oppfolgingsbruker.getAktorId());
        });
    }

    public void avslutt(AktorId aktorId, String veileder, String begrunnelse) {
        transactor.executeWithoutResult((ignored) -> {
            endPeriode(aktorId, veileder, begrunnelse);
            avsluttOppfolging(aktorId);
        });
    }

    public Optional<OppfolgingsperiodeEntity> hentOppfolgingsperiode(String uuid) {
        return queryForNullableObject(
                () -> db.queryForObject(
                hentOppfolingsperioderSQL + "WHERE UUID = ?",
                        OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode, uuid
                )
        );
    }

    public List<OppfolgingsperiodeEntity> hentOppfolgingsperioder(AktorId aktorId) {
        return db.query(hentOppfolingsperioderSQL +
                        "WHERE aktor_id = ?",
                OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode,
                aktorId.get()
        );
    }

    public List<OppfolgingsperiodeEntity> hentAvsluttetOppfolgingsperioder(AktorId aktorId) {
        return db.query(hentOppfolingsperioderSQL +
                        "WHERE aktor_id = ? AND sluttdato is not null",
                OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode,
                aktorId.get()
        );
    }

    public Optional<OppfolgingsperiodeEntity> hentGjeldendeOppfolgingsperiode(AktorId aktorId) {
        return queryForNullableObject(
                () -> db.queryForObject(
                        hentOppfolingsperioderSQL + "WHERE aktor_id = ? AND sluttdato is null",
                        OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode,
                        aktorId.get()
                )
        );
    }

    private void insert(AktorId aktorId, OppfolgingStartBegrunnelse getOppfolgingStartBegrunnelse, NavIdent veileder, StartetAvType startetAvType) {
        db.update("" +
                        "INSERT INTO OPPFOLGINGSPERIODE(uuid, aktor_id, startDato, oppdatert, start_begrunnelse, startet_av, startet_av_type) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)",
                UUID.randomUUID().toString(),
                aktorId.get(),
                getOppfolgingStartBegrunnelse.name(),
                veileder != null ? veileder.get() : null,
                startetAvType.name());
    }

    private void setActive(AktorId aktorId) {
        db.update("UPDATE " +
                        OppfolgingsStatusRepository.TABLE_NAME +
                        " SET " + UNDER_OPPFOLGING + "= 1, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE " + AKTOR_ID + " = ?",
                aktorId.get());
    }

    private void endPeriode(AktorId aktorId, String veileder, String begrunnelse) {
        db.update("" +
                        "UPDATE OPPFOLGINGSPERIODE " +
                        "SET avslutt_veileder = ?, " +
                        "avslutt_begrunnelse = ?, " +
                        "sluttDato = CURRENT_TIMESTAMP, " +
                        "oppdatert = CURRENT_TIMESTAMP " +
                        "WHERE aktor_id = ? " +
                        "AND sluttDato IS NULL",
                veileder,
                begrunnelse,
                aktorId.get());
    }

    private void avsluttOppfolging(AktorId aktorId) {
        db.update("UPDATE " +
                        OppfolgingsStatusRepository.TABLE_NAME +
                        " SET under_oppfolging = 0, "
                        + "veileder = null, "
                        + "ny_for_veileder = 0, "
                        + "gjeldende_manuell_status = null, "
                        + "gjeldende_mal = null, "
                        + "oppdatert = CURRENT_TIMESTAMP "
                        + "WHERE aktor_id = ?",
                aktorId.get()
        );
    }

    private static OppfolgingsperiodeEntity mapTilOppfolgingsperiode(ResultSet result, int row) throws SQLException {
        return OppfolgingsperiodeEntity.builder()
                .uuid(UUID.fromString(result.getString("uuid")))
                .aktorId(result.getString("aktor_id"))
                .veileder(result.getString("avslutt_veileder"))
                .startDato(hentZonedDateTime(result, "startdato"))
                .sluttDato(hentZonedDateTime(result, "sluttdato"))
                .begrunnelse(result.getString("avslutt_begrunnelse"))
                .startetBegrunnelse(EnumUtils.valueOf(OppfolgingStartBegrunnelse.class, result.getString("start_begrunnelse")))
                .build();
    }

}
