package no.nav.veilarboppfolging.service

import no.nav.pto_schema.enums.arena.Formidlingsgruppe.IARBS
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe.IVURD
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe.VURDU
import no.nav.veilarboppfolging.ident.randomAktorId
import no.nav.veilarboppfolging.ident.randomFnr
import no.nav.veilarboppfolging.kafka.TestUtils.localArenaOppfolging
import no.nav.veilarboppfolging.kafka.TestUtils.oppfølgingEntity
import no.nav.veilarboppfolging.kafka.TestUtils.oppfølgingsBrukerEndret
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Optional

class ResolveEndringPaaOppfolgingsbrukerEventTest {

    @Test
    fun `Endring på oppfølgingsbruker som var sykmeldt uten arbeidsgiver og fortsatt er sykmeldt uten arbeidsgiver er IrrelevantEndring`() {
        val aktorId = randomAktorId()
        val nåværendeOppfølgingstatus =
            oppfølgingEntity(aktorId = aktorId.get(), localArenaOppfølging = localArenaOppfolging(kvalifiseringsgruppe = VURDU, formidlingsgruppe = IARBS))
        val oppfolgingsbrukerEndret = oppfølgingsBrukerEndret(fnr = randomFnr().get(), formidlingsgruppe = IARBS, kvalifiseringsgruppe = VURDU)

        val endring = resolveEndringPaaOppfolgingsbrukerEvent(
            endringPaaOppfolgingsBruker = EndringPaaOppfolgingsBruker.from(oppfolgingsbrukerEndret, aktorId),
            nåværendeOppfolgingsstatus = nåværendeOppfølgingstatus,
            getKanReaktiveresIArena = { Optional.of(true) },
        )

        assertThat(endring).isInstanceOf(IrrelevantEndring::class.java)
    }

    @Test
    fun `Endring på oppfølgingsbruker som har blitt sykmeldt uten arbeidsgiver er BleSykmeldtUtenArbeidsgiver`() {
        val aktorId = randomAktorId()
        val nåværendeOppfølgingstatus =
            oppfølgingEntity(aktorId = aktorId.get(), localArenaOppfølging = localArenaOppfolging(kvalifiseringsgruppe = IVURD, formidlingsgruppe = IARBS))
        val oppfolgingsbrukerEndret = oppfølgingsBrukerEndret(fnr = randomFnr().get(), formidlingsgruppe = IARBS, kvalifiseringsgruppe = VURDU)

        val endring = resolveEndringPaaOppfolgingsbrukerEvent(
            endringPaaOppfolgingsBruker = EndringPaaOppfolgingsBruker.from(oppfolgingsbrukerEndret, aktorId),
            nåværendeOppfolgingsstatus = nåværendeOppfølgingstatus,
            getKanReaktiveresIArena = { Optional.of(true) },
        )

        assertThat(endring).isInstanceOf(BleSykmeldtUtenArbeidsgiver::class.java)
    }
}