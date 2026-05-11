package no.nav.veilarboppfolging.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MidlertidigSonarForsøkskaninTest {

    @Test
    fun `Skal kunne legge sammen to tall`() {
        val forsøkskanin = MidlertidigSonarForsøkskanin()
        assertThat(forsøkskanin.leggSammen(2, 2 )).isEqualTo(4)
    }
}