package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord

data class UtmeldingskandidatKafkaPubliseringData(
    val utmeldingshendelseId: UUID,
    val filterkategoriPersonId: UUID,
    val filterhendelse: FilterhendelseRecord,
)
