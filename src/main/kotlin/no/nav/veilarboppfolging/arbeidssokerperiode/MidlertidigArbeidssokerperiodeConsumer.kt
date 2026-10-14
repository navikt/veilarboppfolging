package no.nav.veilarboppfolging.arbeidssokerperiode

import kotlin.jvm.optionals.getOrElse
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
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
        val arbeidssokerperiodeId = arbeidssøkerperiode.id.toString()

        val nyPeriode = arbeidssøkerperiode.avsluttet == null

        if (nyPeriode) {
            logger.info("Lagrer aktiv arbeidssøkerperiode med id $arbeidssokerperiodeId")

            val fnr = Fnr.of(arbeidssøkerperiode.identitetsnummer.toString())
            val aktørId = try {
                authService.getAktorIdOrThrow(fnr)
            } catch (e: Exception) {
                logger.error("Kunne ikke hente aktørId for arbeidssøkerperiode med id $arbeidssokerperiodeId", e)
                if (erProd()) {
                    throw e
                } else {
                    logger.warn("Ignorerer arbeidssøkerperiode med id $arbeidssokerperiodeId i dev")
                    return
                }
            }
            
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

    private fun erProd(): Boolean {
        return EnvironmentUtils.isProduction().getOrElse { false }
    }
}
