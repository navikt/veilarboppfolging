package no.nav.veilarboppfolging.service

import java.time.ZonedDateTime
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2.Companion.builder
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ArbeidssokerperiodeAvsluttetHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ArbeidssøkerPeriodeAvsluttet
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.InaktivertIArenaHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.KandidatForUtmeldingHendelseUtfortAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldEtter28Cron
import no.nav.veilarboppfolging.service.utmelding.IservTrigger
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IservServiceIntegrationTest : IntegrationTest() {
    private val iservFraDato: ZonedDateTime = ZonedDateTime.now()

    @Test
    fun oppdaterUtmeldingsStatus_skalLagreNyIservBruker() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        val brukerV2 = kanskjeIservBruker()
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)

        utmeldingsService.oppdaterUtmeldingsStatus(brukerV2)

        val kanskjeUtmelding = utmeldingRepository.eksisterendeIservBruker(AKTOR_ID)
        assertTrue(kanskjeUtmelding.isPresent)
        val utmelding = kanskjeUtmelding.get()
        assertEquals(AKTOR_ID.get(), utmelding.aktorId)
        assertEquals(iservFraDato.toLocalDate(), utmelding.iservSiden.toLocalDate())
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalOppdatereEksisterendeIservBruker() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        val brukerV2 = kanskjeIservBruker(iservFraDato.plusDays(2), Formidlingsgruppe.ISERV)
        utmeldingRepository.insertUtmeldingTabell(OppdateringFraArena_BleIserv(AKTOR_ID, iservFraDato))
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent)

        utmeldingsService.oppdaterUtmeldingsStatus(brukerV2)

        val kanskjeUtmelding = utmeldingRepository.eksisterendeIservBruker(AKTOR_ID)
        assertTrue(kanskjeUtmelding.isPresent)
        val utmelding = kanskjeUtmelding.get()
        assertEquals(AKTOR_ID.get(), utmelding.aktorId)
        assertEquals(brukerV2.iservFraDato, utmelding.iservSiden.toLocalDate())
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalSletteBrukerSomIkkeLengerErIserv() {
        val brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.IARBS)
        utmeldingRepository.insertUtmeldingTabell(OppdateringFraArena_BleIserv(AKTOR_ID, iservFraDato))
        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent)

        utmeldingsService.oppdaterUtmeldingsStatus(brukerV2)

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)
    }

    @Test
    fun oppdaterUtmeldingsStatus_skalIkkeStarteBrukerSomIkkeHarOppfolgingsstatus() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        val brukerV2 = kanskjeIservBruker(iservFraDato, Formidlingsgruppe.IARBS)

        utmeldingsService.oppdaterUtmeldingsStatus(brukerV2)

        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isEmpty)
    }

    @Test
    fun finnBrukereMedIservI28Dager() {
        assertTrue(utmeldingRepository.finnBrukereMedIservI28Dager().isEmpty())

        insertIservBruker(AktorId.of("0"), iservFraDato.minusDays(30))
        insertIservBruker(AktorId.of("1"), iservFraDato.minusDays(27))
        insertIservBruker(AktorId.of("2"), iservFraDato.minusDays(15))
        insertIservBruker(AktorId.of("3"), iservFraDato)

        assertEquals(1, utmeldingRepository.finnBrukereMedIservI28Dager().size.toLong())
    }

    @Test
    fun avsluttOppfolging() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomISyfo(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.IARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        insertIservBruker(AKTOR_ID, iservFraDato)
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isPresent)

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isPresent)

        val resultat = utmeldingsService.avsluttOppfolgingOgFjernFraUtmeldingsTabell(AKTOR_ID)

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)
        assertEquals(UtmeldEtter28Cron.AvslutteOppfolgingResultat.AVSLUTTET_OK, resultat)
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isEmpty)
    }

    @Test
    fun `migrerBrukereFraGammelUtmeldingstabell - ikke kandidat, kan avsluttes - avslutter oppfølging`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomISyfo(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.IARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        utmeldEtter28Cron.migrerBrukereFraGammelUtmeldingstabell()

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isEmpty)
        val utmeldingskandidatHendelser = kandidatForUtmeldingService.hentUtmeldingsKandidatHendelser(AKTOR_ID)
        assertEquals(0, utmeldingskandidatHendelser.size)
    }

    @Test
    fun `migrerBrukereFraGammelUtmeldingstabell - ikke kandidat, kan ikke avsluttes - lagrer kandidat`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomISyfo(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.IARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = true)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        utmeldEtter28Cron.migrerBrukereFraGammelUtmeldingstabell()

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isPresent)
        val utmeldingskandidatHendelser = kandidatForUtmeldingService.hentUtmeldingsKandidatHendelser(AKTOR_ID)
        assertEquals(1, utmeldingskandidatHendelser.size)
        assertEquals(InaktivertIArenaHendelseType.INAKTIVERT_I_ARENA, utmeldingskandidatHendelser.first().type)
    }

    @Test
    fun `migrerBrukereFraGammelUtmeldingstabell -  ikke under oppfølging - sletter fra utmeldingstabell`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isEmpty)

        utmeldEtter28Cron.migrerBrukereFraGammelUtmeldingstabell()

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)
        val utmeldingskandidatHendelser = kandidatForUtmeldingService.hentUtmeldingsKandidatHendelser(AKTOR_ID)
        assertEquals(0, utmeldingskandidatHendelser.size)
    }

    @Test
    fun `migrerBrukereFraGammelUtmeldingstabell - er kandidat - sletter fra utmeldingstabell`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomISyfo(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.IARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        val oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).get().uuid
        lagreKandidatForUtmelding(FNR, oppfolgingsperiodeId)
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        utmeldEtter28Cron.migrerBrukereFraGammelUtmeldingstabell()

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isPresent)
        val utmeldingskandidatHendelser = kandidatForUtmeldingService.hentUtmeldingsKandidatHendelser(AKTOR_ID)
        assertEquals(1, utmeldingskandidatHendelser.size)
        assertEquals(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT, utmeldingskandidatHendelser.first().type)
    }

    @Test
    fun `migrerBrukereFraGammelUtmeldingstabell - er kandidat som ikke kan avsluttes - sletter fra utmeldingstabell`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomISyfo(AKTOR_ID, FNR)
        setLocalArenaOppfolging(AKTOR_ID, Formidlingsgruppe.IARBS)
        mockTiltakshistorikk(FNR, harAktiveDeltakelser = false)
        mockUngdomsprogram(FNR, erDeltaker = false)
        mockArbeidssoekerregisteret(FNR, erArbeidssoeker = false)
        mockAap(FNR, harAap = false)
        val oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).get().uuid
        kandidatForUtmeldingRepository.lagreKandidatSomIkkeKunneAvsluttesOgHendelse(
            hendelse = ArbeidssøkerPeriodeAvsluttet(
                oppfolgingsperiodeUuid = oppfolgingsperiodeId,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
                utfortAv = "A123123",
                kilde = "arbeidssøkerregisteret",
                arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
                avslutningsarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString(),
                hendelseTidspunkt = ZonedDateTime.now().toInstant(),
            ),
            oppfolgingsperiodeId = oppfolgingsperiodeId,
            begrunnelse = "Har AAP",
        )
        insertIservBruker(AKTOR_ID, iservFraDato.minusDays(30))

        utmeldEtter28Cron.migrerBrukereFraGammelUtmeldingstabell()

        assertTrue(utmeldingRepository.eksisterendeIservBruker(AKTOR_ID).isEmpty)
        assertTrue(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AKTOR_ID).isPresent)
        val utmeldingskandidatHendelser = kandidatForUtmeldingService.hentUtmeldingsKandidatHendelser(AKTOR_ID)
        assertEquals(1, utmeldingskandidatHendelser.size)
        assertEquals(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT, utmeldingskandidatHendelser.first().type)
    }

    private fun kanskjeIservBruker(
        iservFraDato: ZonedDateTime = this.iservFraDato,
        formidlingsgruppe: Formidlingsgruppe = Formidlingsgruppe.ISERV
    ): KanskjeIservBruker {
        return KanskjeIservBruker(
            iservFraDato.toLocalDate(),
            AKTOR_ID,
            formidlingsgruppe,
            IservTrigger.OppdateringPaaOppfolgingsBruker,
            true
        )
    }

    private fun insertIservBruker(aktorId: AktorId, iservFraDato: ZonedDateTime): EndringPaaOppfoelgingsBrukerV2 {
        val brukerV2 = builder()
            .fodselsnummer((nesteFnr++).toString())
            .formidlingsgruppe(Formidlingsgruppe.ISERV)
            .iservFraDato(iservFraDato.toLocalDate())
            .build()

        utmeldingRepository.insertUtmeldingTabell(OppdateringFraArena_BleIserv(aktorId, iservFraDato))

        return brukerV2
    }

    companion object {
        private val FNR: Fnr = Fnr.of("87903794210")
        private val AKTOR_ID: AktorId = AktorId.of("1234")
        private var nesteFnr = 0
    }
}
