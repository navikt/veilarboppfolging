package no.nav.veilarboppfolging.service.utmelding

import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_AlleredeUteAvOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_IkkeLengerIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_NoOp
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArbeidsøkerRegSync_OppdaterIservDato
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_AlleredeUteAvOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_BleIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_IkkeLengerIserv
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_NoOp
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.OppdateringFraArena_OppdaterIservDato
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UpsertIUtmelding
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsHendelse
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import java.time.LocalDate
import java.time.ZoneId

enum class IservTrigger {
    OppdateringPaaOppfolgingsBruker,
    ArbeidssøkerRegistreringSync,
}

data class KanskjeIservBruker(
    val iservFraDato: LocalDate?,
    val aktorId: AktorId,
    val formidlingsgruppe: Formidlingsgruppe,
    val trigger: IservTrigger
) {

    fun resolveUtmeldingsHendelse(erUnderOppfolging: () -> Boolean, erBrukerIUtmeldingsTabell :() -> Boolean): UtmeldingsHendelse {
        return if (erIserv()) {
            if (erUnderOppfolging()) {
                resolveUpsertHendelse(erBrukerIUtmeldingsTabell)
            } else {
                if (erBrukerIUtmeldingsTabell()) {
                    when (trigger) {
                        IservTrigger.ArbeidssøkerRegistreringSync -> ArbeidsøkerRegSync_AlleredeUteAvOppfolging(aktorId)
                        IservTrigger.OppdateringPaaOppfolgingsBruker -> OppdateringFraArena_AlleredeUteAvOppfolging(aktorId)
                    }
                } else {
                    when (trigger) {
                        IservTrigger.ArbeidssøkerRegistreringSync -> ArbeidsøkerRegSync_NoOp(aktorId)
                        IservTrigger.OppdateringPaaOppfolgingsBruker -> OppdateringFraArena_NoOp(aktorId)
                    }
                }
            }
        } else {
            if (erBrukerIUtmeldingsTabell()) {
                when (trigger) {
                    IservTrigger.OppdateringPaaOppfolgingsBruker -> OppdateringFraArena_IkkeLengerIserv(aktorId)
                    IservTrigger.ArbeidssøkerRegistreringSync -> ArbeidsøkerRegSync_IkkeLengerIserv(aktorId)
                }
            } else {
                when (trigger) {
                    IservTrigger.ArbeidssøkerRegistreringSync -> ArbeidsøkerRegSync_NoOp(aktorId)
                    IservTrigger.OppdateringPaaOppfolgingsBruker -> OppdateringFraArena_NoOp(aktorId)
                }
            }
        }
    }

    private fun resolveUpsertHendelse(erBrukerIUtmeldingsTabell :() -> Boolean): UpsertIUtmelding {
        if (iservFraDato == null) {
            secureLog.error("Kan ikke oppdatere utmeldingstabell med bruker siden iservFraDato mangler. aktorId=$aktorId");
            throw IllegalArgumentException("iservFraDato mangler på EndringPaaOppfoelgingsBrukerV2");
        }
        return when (erBrukerIUtmeldingsTabell()) {
            true ->  {
                when (trigger) {
                    IservTrigger.ArbeidssøkerRegistreringSync -> ArbeidsøkerRegSync_OppdaterIservDato(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()))
                    IservTrigger.OppdateringPaaOppfolgingsBruker -> OppdateringFraArena_OppdaterIservDato(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()))
                }
            }
            else -> {
                when (trigger) {
                    IservTrigger.ArbeidssøkerRegistreringSync ->  ArbeidsøkerRegSync_BleIserv(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()))
                    IservTrigger.OppdateringPaaOppfolgingsBruker ->  OppdateringFraArena_BleIserv(aktorId, iservFraDato.atStartOfDay(ZoneId.systemDefault()))
                }
            }
        }
    }

    private fun erIserv() = formidlingsgruppe == Formidlingsgruppe.ISERV

    companion object {
        fun of(bruker: EndringPaaOppfoelgingsBrukerV2, aktorId: AktorId): KanskjeIservBruker {
            return KanskjeIservBruker(bruker.iservFraDato, aktorId, bruker.formidlingsgruppe, IservTrigger.OppdateringPaaOppfolgingsBruker)
        }
    }
}
