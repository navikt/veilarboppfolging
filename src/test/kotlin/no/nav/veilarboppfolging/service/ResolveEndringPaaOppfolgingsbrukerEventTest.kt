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
import no.nav.pto_schema.enums.arena.Formidlingsgruppe.ISERV
import no.nav.pto_schema.enums.arena.Formidlingsgruppe.ARBS
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe.BATT

class ResolveEndringPaaOppfolgingsbrukerEventTest {

    @Test
    fun `Endring på oppfølgingsbruker som var sykmeldt uten arbeidsgiver og fortsatt er sykmeldt uten arbeidsgiver er IrrelevantEndring`() {
        val aktorId = randomAktorId()
        val nåværendeOppfølgingstatus =
            oppfølgingEntity(aktorId = aktorId.get(), localArenaOppfølging = localArenaOppfolging(kvalifiseringsgruppe = VURDU, formidlingsgruppe = IARBS))

        val endring = resolveEndringPaaOppfolgingsbrukerEvent(
            formidlingsgruppe = IARBS,
            nåværendeOppfolgingsstatus = nåværendeOppfølgingstatus,
        )

        assertThat(endring).isInstanceOf(IrrelevantEndring::class.java)
    }

    @Test
    fun `Endring på oppfølgingsbruker som har blitt sykmeldt uten arbeidsgiver er IrrelevantEndring`() {
        val aktorId = randomAktorId()
        val nåværendeOppfølgingstatus =
            oppfølgingEntity(aktorId = aktorId.get(), localArenaOppfølging = localArenaOppfolging(kvalifiseringsgruppe = IVURD, formidlingsgruppe = IARBS))

        val endring = resolveEndringPaaOppfolgingsbrukerEvent(
            formidlingsgruppe = IARBS,
            nåværendeOppfolgingsstatus = nåværendeOppfølgingstatus,
        )

        assertThat(endring).isInstanceOf(IrrelevantEndring::class.java)
    }

    @Test
    fun `Endring på oppfølgingsbruker som har gått fra ARBS til ISERV er VarArbsBleIserv`() {
        val aktorId = randomAktorId()
        val nåværendeOppfølgingstatus =
            oppfølgingEntity(aktorId = aktorId.get(), localArenaOppfølging = localArenaOppfolging(kvalifiseringsgruppe = BATT, formidlingsgruppe = ARBS))

        val endring = resolveEndringPaaOppfolgingsbrukerEvent(
            formidlingsgruppe = ISERV,
            nåværendeOppfolgingsstatus = nåværendeOppfølgingstatus,
        )

        assertThat(endring).isInstanceOf(BleInaktivertVarArbs::class.java)
    }
}