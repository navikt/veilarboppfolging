package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.postgresql.util.PGobject
import java.time.Instant
import java.util.UUID

class OppfolgingAvsluttetHendelse(
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType = KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
    utfortAv: String? = "veilarboppfolging",
    kilde: String = "veilarboppfolging",
    hendelseTidspunkt: Instant = Instant.now(),
    val oppfolgingAvsluttetHendelseType: OppfolgingAvsluttetHendelseType,
) :
    KandidatForUtmeldingHendelse(
        oppfolgingsperiodeUuid,
        utfortAvType,
        utfortAv,
        kilde,
        hendelseTidspunkt
    ) {
    override val type:  OppfolgingAvsluttetHendelseType = oppfolgingAvsluttetHendelseType
    override val hendelseDataJson: PGobject? = null
    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = Operasjon.STOPP,
            hendelse = null
        )
    }
}

enum class OppfolgingAvsluttetHendelseType : KandidatForUtmeldingHendelseType {
    OPPFOLGING_AVSLUTTET_AUTOMATISK,
    OPPFOLGING_AVSLUTTET_MANUELT
}
