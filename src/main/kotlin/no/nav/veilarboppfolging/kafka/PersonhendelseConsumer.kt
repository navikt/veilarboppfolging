package no.nav.veilarboppfolging.kafka

import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.client.pdl.eeaLand
import no.nav.veilarboppfolging.ident.Ident
import no.nav.veilarboppfolging.ident.ValidIdent
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.EndretFolkeregisterstatus
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.EndretFolkeregisterstatusHendelseType
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Dodsfall
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.utils.SecureLog.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import no.nav.veilarboppfolging.ident.AktorId as IdentAktorId

@Service
class PersonhendelseConsumer(
    private val oppfolgingService: OppfolgingService,
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val aktorOppslagClient: AktorOppslagClient,
    private val folkeregisterStatusClient: PdlFolkeregisterStatusClient,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
) {
    val logger = LoggerFactory.getLogger(PersonhendelseConsumer::class.java)

    fun consumePersonhendelse(kafkaMelding: ConsumerRecord<String, Personhendelse?>) {
        val personhendelse = kafkaMelding.value() ?: return
        if (personhendelse.gjelderDodsfall()) {
            handterDodsfall(personhendelse)
        } else if (personhendelse.kanGjeldeLovligOpphold()) {
            handterLovligOpphold(personhendelse)
        }
    }

    private fun handterDodsfall(personhendelse: Personhendelse) {
        val aktorIder = getAktorIder(personhendelse)

        aktorIder.forEach { aktorId ->
            val oppfolgingsperiodeId = oppfolgingService.hentGjeldendeOppfolgingsperiode(aktorId).getOrNull()?.uuid
            if (oppfolgingsperiodeId != null) {
                logger.info("Forsøker å avslutte oppfølgingsperiode $oppfolgingsperiodeId pga dødsfall")
                avsluttOppfolgingService.avsluttOppfolgingHvisKanAvsluttes(Dodsfall(aktorId))
            } else {
                secureLog.info("Ingen oppfølgingsperiode å avslutte for ident $aktorId pga dødsfall")
            }
        }
    }

    private fun handterLovligOpphold(personhendelse: Personhendelse) {
        val aktorId = getAktorIder(personhendelse).firstOrNull() ?: throw IllegalStateException("Ingen aktørid funnet for personhendelse")
        val gjeldendeFnr = try {
            aktorOppslagClient.hentFnr(aktorId)
        } catch (e: Exception) {
            logger.error("Fant ikke gjeldende ident for aktørid for hendelse relatert til lovlig opphold", e)
            if (isDevelopment().getOrElse { false }) {
                logger.warn("Ignorerer feil ved oppslag av fnr for aktørid i dev")
                return
            } else {
                throw RuntimeException("Feil ved oppslag av fnr for aktørid", e)
            }
        }
        val oppfolgingsperiodeId = oppfolgingService.hentGjeldendeOppfolgingsperiode(gjeldendeFnr).getOrNull()?.uuid
        if (oppfolgingsperiodeId != null) {
            logger.info("Har mottatt hendelse som kan påvirke lovlig opphold for oppfølgingsperiodeId $oppfolgingsperiodeId")
            val folkeregisterStatus = folkeregisterStatusClient.hentFolkeregisterStatus(gjeldendeFnr)
            when (folkeregisterStatus.fregStatus) {
                ForenkletFolkeregisterStatus.forsvunnet,
                ForenkletFolkeregisterStatus.opphoert,
                ForenkletFolkeregisterStatus.ingen_status,
                ForenkletFolkeregisterStatus.ukjent-> lagUtmeldingskandidat(
                    fnr = gjeldendeFnr,
                    oppfolgingsperiodeId = oppfolgingsperiodeId,
                    hendelseTidspunkt = personhendelse.opprettet,
                    endretFolkeregisterstatusHendelseType = folkeregisterStatus.fregStatus.toEndretFolkeregisterstatusHendelseType(),
                )
                else -> logger.info("Bruker med oppfølgingsperiode $oppfolgingsperiodeId har ingen relevante endringer i folkeregisterstatus")
            }
        }
    }

    private fun lagUtmeldingskandidat(fnr: Fnr, oppfolgingsperiodeId: UUID, hendelseTidspunkt: Instant, endretFolkeregisterstatusHendelseType: EndretFolkeregisterstatusHendelseType) {
        logger.info("Oppretter kandidat for utmelding for oppfølgingsperiodeId $oppfolgingsperiodeId pga endring i folkeregisterstatus")
        kandidatForUtmeldingService.handterUtmeldingsHendelse(
            fnr = fnr,
            hendelse = EndretFolkeregisterstatus(
                oppfolgingsperiodeUuid = oppfolgingsperiodeId,
                hendelseTidspunkt = hendelseTidspunkt,
                endretFolkeregisterstatusHendelseType = endretFolkeregisterstatusHendelseType,
            ),
        )
    }

    private fun Personhendelse.gjelderDodsfall(): Boolean {
        return doedsfall?.doedsdato != null &&
                (endringstype == Endringstype.OPPRETTET || endringstype == Endringstype.KORRIGERT)
    }

    private fun Personhendelse.kanGjeldeLovligOpphold(): Boolean {
        val harFattStatsborgerskapILandUtenforEUellerEOS = statsborgerskap?.land != null && !eeaLand.contains(statsborgerskap.land) &&
                    (endringstype == Endringstype.OPPRETTET || endringstype == Endringstype.KORRIGERT)
        val harFlyttetUt = utflyttingFraNorge != null && (endringstype == Endringstype.OPPRETTET || endringstype == Endringstype.KORRIGERT)
        val harOpphortBostedsadresse = (opplysningstype == "BostedsadresseV1" || bostedsadresse != null) && endringstype == Endringstype.OPPHOERT

        return harFattStatsborgerskapILandUtenforEUellerEOS || harFlyttetUt || harOpphortBostedsadresse
    }

    private fun getAktorIder(personhendelse: Personhendelse): List<AktorId> {
        // personidenter vil inneholde alle former for identer på brukeren, både nåværende og historiske
        return personhendelse.personidenter
            .map { Ident.validate(it) }
            .filterIsInstance<ValidIdent>()
            .map { it.ident }
            .filterIsInstance<IdentAktorId>()
            .map { AktorId.of(it.value) }
    }

    private fun ForenkletFolkeregisterStatus.toEndretFolkeregisterstatusHendelseType(): EndretFolkeregisterstatusHendelseType {
        return when (this) {
            ForenkletFolkeregisterStatus.forsvunnet -> EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_FORSVUNNET
            ForenkletFolkeregisterStatus.opphoert -> EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_OPPHORT
            ForenkletFolkeregisterStatus.ukjent -> EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_UKJENT
            ForenkletFolkeregisterStatus.ingen_status -> EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_INGEN_STATUS
            else -> throw IllegalArgumentException("ForenkletFolkeregisterStatus $this kan ikke konverteres til EndretFolkeregisterstatusHendelseType")
        }
    }
}