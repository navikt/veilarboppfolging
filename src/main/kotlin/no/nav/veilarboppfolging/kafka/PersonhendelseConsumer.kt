package no.nav.veilarboppfolging.kafka

import kotlin.jvm.optionals.getOrNull
import no.nav.common.types.identer.AktorId
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.veilarboppfolging.ident.Ident
import no.nav.veilarboppfolging.ident.ValidIdent
import no.nav.veilarboppfolging.ident.AktorId as IdentAktorId
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Dodsfall
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersonhendelseConsumer(
    private val oppfolgingService: OppfolgingService,
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
) {
    val logger = LoggerFactory.getLogger(PersonhendelseConsumer::class.java)

    fun consumePersonhendelse(kafkaMelding: ConsumerRecord<Long, Personhendelse?>) {
        val personhendelse = kafkaMelding.value() ?: return
        if (personhendelse.gjelderDodsfall()) {
            // personidenter vil inneholde alle former for identer på brukeren, både nåværende og historiske
            val aktorIder = personhendelse.personidenter
                .map { Ident.validate(it) }
                .filterIsInstance<ValidIdent>()
                .map { it.ident }
                .filterIsInstance<IdentAktorId>()
                .map { AktorId.of(it.value) }

            aktorIder.forEach { aktorId ->
                val oppfolgingsperiodeId = oppfolgingService.hentGjeldendeOppfolgingsperiode(aktorId).getOrNull()?.uuid
                if (oppfolgingsperiodeId != null) {
                    logger.info("Forsøker å avslutte oppfølgingsperiode $oppfolgingsperiodeId pga dødsfall")
                    avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(Dodsfall(aktorId))
                } else {
                    secureLog.info("Ingen oppfølgingsperiode å avslutte for ident $aktorId pga dødsfall, ${kafkaMelding.key()}")
                }
            }
        }
    }

    private fun Personhendelse.gjelderDodsfall(): Boolean {
        return doedsfall?.doedsdato != null &&
                (endringstype == Endringstype.OPPRETTET || endringstype == Endringstype.KORRIGERT)
    }
}