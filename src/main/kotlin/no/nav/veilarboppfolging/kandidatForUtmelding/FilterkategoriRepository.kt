package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class FilterkategoriRepository(
    private val db: NamedParameterJdbcTemplate
) {
    fun hentEllerOpprettFilterhendelseId(oppfolgingsperiodeId: UUID): UUID {
        val sql = """
            INSERT INTO filterkategori_id_mapping(kategori, oppfolgingsperiode_id, filterkategori_person_id)
            VALUES ('KANDIDAT_FOR_UTMELDING', :oppfolgingsperiodeId, gen_random_uuid())
            ON CONFLICT (oppfolgingsperiode_id, kategori) 
            DO UPDATE SET oppfolgingsperiode_id = EXCLUDED.oppfolgingsperiode_id
            RETURNING filterkategori_person_id
        """.trimIndent()
        return db.queryForObject(
            sql, mapOf(
                "oppfolgingsperiodeId" to oppfolgingsperiodeId.toString(),
            )
        ) { rs, _ -> UUID.fromString(rs.getString("filterkategori_person_id")) }!!
    }

    fun hentFilterhendelseId(oppfolgingsperiodeId: UUID): UUID? {
        return db.query(
            """
            SELECT filterkategori_person_id
            FROM filterkategori_id_mapping
            WHERE kategori = 'KANDIDAT_FOR_UTMELDING' AND oppfolgingsperiode_id = :oppfolgingsperiodeId
        """.trimIndent(),
            mapOf(
                "oppfolgingsperiodeId" to oppfolgingsperiodeId.toString(),
            ),
        ) { rs, _ -> UUID.fromString(rs.getString("filterkategori_person_id")) }.firstOrNull()
    }
}
