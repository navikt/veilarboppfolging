package no.nav.veilarboppfolging.kafka.inngang

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.client.veilarbarena.ArenaRegistreringResultat
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaError
import no.nav.veilarboppfolging.client.veilarbarena.RegistrerIArenaSuccess
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.DOD
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_OK
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.IKKE_LOVLIG_OPPHOLD
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.INGEN_STATUS_FOLKEREGISTERET
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.UKJENT_STATUS_FOLKEREGISTERET
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.toKanStarteOppfolging
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StartOppfolgingConsumerService(
    private val pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient,
    private val startOppfolgingService: StartOppfolgingService,
    private val arenaOppfolgingService: ArenaOppfolgingService,
    private val authService: AuthService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun consumeStartOppfolging(kafkaMelding: ConsumerRecord<String, StartOppfolgingMelding>) {
        val fnr = Fnr.ofValidFnr(kafkaMelding.key())
        val aktorId = authService.getAktorIdOrThrow(fnr)
        val startOppfolgingMelding = kafkaMelding.value()

        if (kanStarteOppfolging(fnr)) {
            val arenaRespons = startOppfolgingIArena(fnr)
            when (arenaRespons.arenaResultat.kode) {
                ArenaRegistreringResultat.KAN_REAKTIVERES_FORENKLET -> {
                    logger.warn("Kan ikke starte oppfølging i Arena fordi bruker kan enkelt reaktiveres: ${arenaRespons.arenaResultat.kode}")
                    return
                }
                ArenaRegistreringResultat.FNR_FINNES_IKKE,
                ArenaRegistreringResultat.UKJENT_FEIL -> {
                    logger.error("Feil ved registrering av bruker i Arena: ${arenaRespons.arenaResultat.resultat}")
                    throw RuntimeException("Feil ved registrering av bruker i Arena: ${arenaRespons.arenaResultat.resultat}")
                }
                else -> {
                    logger.info("Bruker registrert i Arena med resultat: ${arenaRespons.arenaResultat.kode}")
                }
            }
            val oppfolgingsbruker = OppfolgingsRegistrering.systemRegistrering(
                fnr = fnr,
                aktorId = aktorId,
                registrant = startOppfolgingMelding.registrant.toOppfolgingsRegistrant(),
                oppfolgingStartBegrunnelseFraSystem = startOppfolgingMelding.aarsak.toOppfolgingStartBegrunnelseFraSystem(),
                kontor = startOppfolgingMelding.arbeidsoppfolgingskontor,
            )
            startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker)
        } else {
            logger.warn("Kan ikke starte oppfølging for bruker pga folkeregisterstatus")
        }
    }

    private fun kanStarteOppfolging(fnr: Fnr): Boolean {
        val folkeregisterstatus = pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)
        val kanStarteOppfolging = folkeregisterstatus.toKanStarteOppfolging()
        val erUnder18 = folkeregisterstatus.under18

        if (erUnder18) {
            logger.warn("Kan ikke starte oppfølging for bruker under 18 år")
            return false
        }

        return when (kanStarteOppfolging) {
            DOD,
            FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS,
            FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT,
            IKKE_LOVLIG_OPPHOLD,
            INGEN_STATUS_FOLKEREGISTERET,
            UKJENT_STATUS_FOLKEREGISTERET -> {
                logger.warn("Kan ikke starte oppfølging for bruker med folkeregisterstatus: $kanStarteOppfolging")
                false
            }
            FREG_STATUS_OK -> true
        }
    }

    private fun startOppfolgingIArena(fnr: Fnr): RegistrerIArenaSuccess {
        val arenaResponse = arenaOppfolgingService.registrerIkkeArbeidssoker(fnr)
        when (arenaResponse) {
            is RegistrerIArenaSuccess -> {
                return arenaResponse
            }
            is RegistrerIArenaError -> {
                logger.error("Feil ved registrering av bruker i Arena", arenaResponse.throwable)
                throw RuntimeException(arenaResponse.message, arenaResponse.throwable.cause)
            }
        }
    }
}
