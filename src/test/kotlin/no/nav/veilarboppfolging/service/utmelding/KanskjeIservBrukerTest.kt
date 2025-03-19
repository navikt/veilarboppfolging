package no.nav.veilarboppfolging.service.utmelding

import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_AlleredeUteAvOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_IkkeLengerIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_NoOp
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_OppdaterIservDato
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.InsertIUtmelding
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.NoOp
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.SlettFraUtmelding
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UpdateIservDatoUtmelding
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class KanskjeIservBrukerTest {

    val AKTOR_ID = AktorId.of("123123123")
    val ISERV_FRA_DATO = LocalDate.now()

    val brukerIArbs = KanskjeIservBruker(
        iservFraDato = ISERV_FRA_DATO,
        aktorId = AKTOR_ID,
        formidlingsgruppe = Formidlingsgruppe.ARBS,
        trigger = IservTrigger.OppdateringPaaOppfolgingsBruker
    )

    val brukerIServ = KanskjeIservBruker(
        iservFraDato = ISERV_FRA_DATO,
        aktorId = AKTOR_ID,
        formidlingsgruppe = Formidlingsgruppe.ISERV,
        trigger = IservTrigger.OppdateringPaaOppfolgingsBruker
    )

    @Test
    fun `Brukere som ikke er ISERV og ikke i utmeldingstabell skal ikke gjøres noe med`() {
        val utmeldingsHendelse = brukerIArbs.resolveUtmeldingsHendelse({ true }, { false })
        assertInstanceOf<NoOp>(utmeldingsHendelse)
    }

    @Test
    fun `Brukere som er ISERV, ikke under oppfølging og ikke i utmeldingstabell skal ikke gjøres noe med`() {
        val utmeldingsHendelse = brukerIServ.resolveUtmeldingsHendelse({ false }, { false })
        assertInstanceOf<NoOp>(utmeldingsHendelse)
    }

    @Test
    fun `Brukere som er ISERV, er under oppfølging og ikke i utmeldingstabell skal insertes i utmelding`() {
        val utmeldingsHendelse = brukerIServ.resolveUtmeldingsHendelse({ true }, { false })
        assertInstanceOf<InsertIUtmelding>(utmeldingsHendelse)
    }

    @Test
    fun `Brukere som er ISERV, er under oppfølging og er i utmeldingstabell skal oppdatere Iserv dato i utmelding`() {
        val utmeldingsHendelse = brukerIServ.resolveUtmeldingsHendelse({ true }, { true })
        assertInstanceOf<UpdateIservDatoUtmelding>(utmeldingsHendelse)
    }

    @Test
    fun `Brukere som er ISERV, er ikke under oppfølging og er i utmeldingstabell skal slettes fra utmelding`() {
        val utmeldingsHendelse = brukerIServ.resolveUtmeldingsHendelse({ false }, { true })
        assertInstanceOf<SlettFraUtmelding>(utmeldingsHendelse)
    }

    @Test
    fun `Brukere som ikke er ISERV, er under oppfølging og er i utmeldingstabell skal slettes fra utmelding`() {
        val utmeldingsHendelse = brukerIArbs.resolveUtmeldingsHendelse({ true }, { true })
        assertInstanceOf<SlettFraUtmelding>(utmeldingsHendelse)
    }

    @Test
    fun `Brukere som ikke er ISERV, ikke er under oppfølging og er i utmeldingstabell skal slettes fra utmelding`() {
        val utmeldingsHendelse = brukerIArbs.resolveUtmeldingsHendelse({ false }, { true })
        assertInstanceOf<SlettFraUtmelding>(utmeldingsHendelse)
    }

    @Test
    fun `Brukere som ikke er ISERV, ikke er under oppfølging og ikke er i utmeldingstabell skal slettes fra utmelding`() {
        val utmeldingsHendelse = brukerIArbs.resolveUtmeldingsHendelse({ false }, { false })
        assertInstanceOf<NoOp>(utmeldingsHendelse)
    }

    @Test
    fun `Skal kaste exception hvis bruker er Iserv og under oppfølging og iservFraDato mangler`() {
        assertThrows<IllegalArgumentException> {
            brukerIServ.copy(iservFraDato = null).resolveUtmeldingsHendelse({ true }, { false })
        }
    }

    @Test
    fun `Skal sette ArbeidsøkerRegSync som trigger`() {
        val iservBruker = brukerIServ.copy(trigger = IservTrigger.ArbeidssøkerRegistreringSync)
        iservBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { false }, erBrukerIUtmeldingsTabell = { false }).let { assertInstanceOf<ArbeidsøkerRegSync_NoOp>(it) }
        iservBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { true }, erBrukerIUtmeldingsTabell = { false }).let { assertInstanceOf<ArbeidsøkerRegSync_BleIserv>(it) }
        iservBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { false }, erBrukerIUtmeldingsTabell = { true }).let { assertInstanceOf<ArbeidsøkerRegSync_AlleredeUteAvOppfolging>(it) }
        iservBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { true }, erBrukerIUtmeldingsTabell = { true }).let { assertInstanceOf<ArbeidsøkerRegSync_OppdaterIservDato>(it) }
        val arbsBruker = brukerIArbs.copy(trigger = IservTrigger.ArbeidssøkerRegistreringSync)
        arbsBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { true }, erBrukerIUtmeldingsTabell = { true }).let { assertInstanceOf<ArbeidsøkerRegSync_IkkeLengerIserv>(it)  }
        arbsBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { false }, erBrukerIUtmeldingsTabell = { true }).let { assertInstanceOf<ArbeidsøkerRegSync_IkkeLengerIserv>(it)  }
        arbsBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { true }, erBrukerIUtmeldingsTabell = { false }).let { assertInstanceOf<ArbeidsøkerRegSync_NoOp>(it)  }
        arbsBruker.resolveUtmeldingsHendelse(erUnderOppfolging = { false }, erBrukerIUtmeldingsTabell = { false }).let { assertInstanceOf<ArbeidsøkerRegSync_NoOp>(it)  }
    }

}