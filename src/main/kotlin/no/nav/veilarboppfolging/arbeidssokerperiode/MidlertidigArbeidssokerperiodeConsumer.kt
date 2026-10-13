package no.nav.veilarboppfolging.arbeidssokerperiode

import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.veilarboppfolging.service.AuthService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class MidlertidigArbeidssokerperiodeConsumer(
    private val authService: AuthService,
    private val aktivArbeidssokerperiodeRepository: AktivArbeidssokerperiodeRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    open fun consumeArbeidssokerperiode(kafkaMelding: ConsumerRecord<String, Periode>) {
        val arbeidssøkerperiode: Periode = kafkaMelding.value()

        val fnr = Fnr.of(arbeidssøkerperiode.identitetsnummer.toString())
        val aktørId = authService.getAktorIdOrThrow(fnr)

        val arbeidssokerperiodeId = arbeidssøkerperiode.id.toString()

        val nyPeriode = arbeidssøkerperiode.avsluttet == null
        if (nyPeriode) {
            logger.info("Lagrer aktiv arbeidssøkerperiode med id $arbeidssokerperiodeId")
            aktivArbeidssokerperiodeRepository.lagreAktivArbeidssokerperiode(
                AktivArbeidssokerperiode(
                    arbeidssokerperiodeId = arbeidssokerperiodeId,
                    aktorId = aktørId,
                    fnr = fnr,
                    startetTidspunkt = arbeidssøkerperiode.startet.tidspunkt,
                )
            )
        } else {
            logger.info("Sletter avsluttet arbeidssøkerperiode med id $arbeidssokerperiodeId")
            aktivArbeidssokerperiodeRepository.slettAktivArbeidssokerperiode(arbeidssokerperiodeId)
        }
    }
}
