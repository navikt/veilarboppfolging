package no.nav.veilarboppfolging.arbeidssokerperiode

import java.time.Instant
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.test.DbTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class AktivArbeidssokerperiodeRepositoryTest {
    private val jdbcTemplate = LocalDatabaseSingleton.jdbcTemplate
    private val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
    private val aktivArbeidssokerperiodeRepository = AktivArbeidssokerperiodeRepository(namedJdbcTemplate)
    private val aktorId = AktorId.of("4321")
    private val fnr = Fnr.of("1111119999")

    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun `erArbeidssoker - er true hvis perioden finnes`() {
        val aktivArbeidssokerperiode = AktivArbeidssokerperiode(
            arbeidssokerperiodeId = UUID.randomUUID().toString(),
            aktorId = aktorId,
            fnr = fnr,
            startetTidspunkt = Instant.now(),
        )
        aktivArbeidssokerperiodeRepository.lagreAktivArbeidssokerperiode(aktivArbeidssokerperiode)

        assertThat(aktivArbeidssokerperiodeRepository.erArbeidssoker(aktorId)).isTrue
    }

    @Test
    fun `erArbeidssoker - er false hvis perioden ikke finnes`() {
        val aktivArbeidssokerperiode = AktivArbeidssokerperiode(
            arbeidssokerperiodeId = UUID.randomUUID().toString(),
            aktorId = aktorId,
            fnr = fnr,
            startetTidspunkt = Instant.now(),
        )
        aktivArbeidssokerperiodeRepository.lagreAktivArbeidssokerperiode(aktivArbeidssokerperiode)
        aktivArbeidssokerperiodeRepository.slettAktivArbeidssokerperiode(aktivArbeidssokerperiode.arbeidssokerperiodeId)

        assertThat(aktivArbeidssokerperiodeRepository.erArbeidssoker(aktorId)).isFalse
    }
}