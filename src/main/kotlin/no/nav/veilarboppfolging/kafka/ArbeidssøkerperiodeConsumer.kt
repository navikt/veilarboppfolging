package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.*
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe.*
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidssøkerperiodeConsumer(
    private val oppfolgingService: OppfolgingService,
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun consumeArbeidssøkerperiode(kafkaMelding: ConsumerRecord<String, Periode>) {
        val arbeidssøkerperiode = kafkaMelding.value()
        val fnr = Fnr.of(arbeidssøkerperiode.identitetsnummer.toString())
        val aktørId = authService.getAktorIdOrThrow(fnr)

        val nyPeriode = arbeidssøkerperiode.avsluttet == null

        if (nyPeriode) {
            val innsatsgruppe = null // TODO: Hent faktisk innsatsgruppe eller profilertTil? Må løse dette
            val arbeidssøker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktørId, innsatsgruppe)
            // TODO: Hvilken metode skal vi egentlig bruke, kanskje en metode i aktiverBrukerService?
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(arbeidssøker, fnr)
            logger.info("Startet oppfølgingsperiode basert på ny arbeidssøkerperiode")
        } else {
            val slutt = arbeidssøkerperiode.avsluttet
            oppfolgingService.avsluttOppfolgingGrunnetAvsluttetArbeidssøkerperiode(aktørId, slutt.aarsak.toString())
        }
    }

    private fun ProfilertTil.tilInnsatsgruppe(): Innsatsgruppe? =
        when (this) {
            UKJENT_VERDI, UDEFINERT -> null
            ANTATT_GODE_MULIGHETER -> STANDARD_INNSATS
            ANTATT_BEHOV_FOR_VEILEDNING -> SITUASJONSBESTEMT_INNSATS
            OPPGITT_HINDRINGER -> BEHOV_FOR_ARBEIDSEVNEVURDERING
        }
}
