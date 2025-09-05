package no.nav.veilarboppfolging.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ManuellRegistrering;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import no.nav.veilarboppfolging.utils.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
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
            "SELECT uuid, aktor_id, avslutt_veileder, startdato, sluttdato, avslutt_begrunnelse, start_begrunnelse, startet_av, startet_av_type " +
                    "FROM OPPFOLGINGSPERIODE ";

    @Autowired
    public OppfolgingsPeriodeRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
        this.transactor = transactor;
    }

    public void start(OppfolgingsRegistrering oppfolgingsbruker) {
        transactor.executeWithoutResult((ignored) -> {
            var kontorSattAvVeileder = oppfolgingsbruker instanceof ManuellRegistrering ? ((ManuellRegistrering) oppfolgingsbruker).getKontorSattAvVeileder() : null;
            insert(
                    oppfolgingsbruker.getAktorId(),
                    oppfolgingsbruker.getOppfolgingStartBegrunnelse(),
                    oppfolgingsbruker.getRegistrertAv().getIdent(),
                    oppfolgingsbruker.getRegistrertAv().getType(),
                    kontorSattAvVeileder
            );
            setActive(oppfolgingsbruker.getAktorId());
        });
    }

    public void avsluttSistePeriodeOgAvsluttOppfolging(AktorId aktorId, String veileder, String begrunnelse) {
        transactor.executeWithoutResult((ignored) -> {
            endPeriode(aktorId, veileder, begrunnelse);
            avsluttOppfolging(aktorId);
        });
    }

    public OppfolgingsperiodeEntity avsluttOppfolgingsperiode(UUID uuid, String veileder, String begrunnelse, ZonedDateTime sluttDato) {
        return transactor.execute((ignored) -> {
            Timestamp sluttTimestamp = Timestamp.from(sluttDato.toInstant());
            return db.queryForObject(""" 
                            UPDATE OPPFOLGINGSPERIODE
                            SET avslutt_veileder = ?,
                            avslutt_begrunnelse = ?,
                            sluttDato = ?,
                            oppdatert = CURRENT_TIMESTAMP
                            WHERE uuid = ?
                            AND sluttDato IS NULL
                            RETURNING *
                            """,
                    OppfolgingsPeriodeRepository::mapTilOppfolgingsperiode,
                    veileder,
                    begrunnelse,
                    sluttTimestamp,
                    uuid.toString());
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

    private void insert(AktorId aktorId, OppfolgingStartBegrunnelse getOppfolgingStartBegrunnelse, @Nullable String startetAvIdent, StartetAvType startetAvType, @Nullable String kontorSattAvVeileder) {
        db.update("" +
                        "INSERT INTO OPPFOLGINGSPERIODE(uuid, aktor_id, startDato, oppdatert, start_begrunnelse, startet_av, startet_av_type, kontor_satt_av_veileder) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                aktorId.get(),
                getOppfolgingStartBegrunnelse.name(),
                startetAvIdent,
                startetAvType.name(),
                kontorSattAvVeileder
        );
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
        var startetAvTypeString = result.getString("startet_av_type");
        var startetAvType = startetAvTypeString != null ? EnumUtils.valueOf(StartetAvType.class, startetAvTypeString) : null;
        return OppfolgingsperiodeEntity.builder()
                .uuid(UUID.fromString(result.getString("uuid")))
                .aktorId(result.getString("aktor_id"))
                .avsluttetAv(result.getString("avslutt_veileder"))
                .startDato(hentZonedDateTime(result, "startdato"))
                .sluttDato(hentZonedDateTime(result, "sluttdato"))
                .begrunnelse(result.getString("avslutt_begrunnelse"))
                .startetAvType(startetAvType)
                .startetAv(result.getString("startet_av"))
                .startetBegrunnelse(EnumUtils.valueOf(OppfolgingStartBegrunnelse.class, result.getString("start_begrunnelse")))
                .build();
    }

}
