package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

data class KanAvsluttesInput(
    val erUnderOppfolging: Boolean,
    val erIservIArena: Boolean,
    val harAktiveTiltaksdeltakelser: Boolean,
    val erDeltakerIUngdomsprogrammet: Boolean,
    val erArbeidssoeker: Boolean,
    val harAap: Boolean,
    val underKvp: Boolean,
    val erOppfolgingForlenget: Boolean,
)

sealed class KunneAvsluttesResultat(val kanAvsluttesInput: KanAvsluttesInput) {

    companion object {

        fun kanAvsluttes(
            avregistrering: Avregistrering,
            input: KanAvsluttesInput,
        ): KunneAvsluttesResultat {
            val avregistreringsType = avregistrering.getAvregistreringsType()
            val erIservIArena = input.erIservIArena
            val kunneIkkeAvslutteBegrunnelse = kanAvsluttesIntern(input, avregistreringsType)
            return when (kunneIkkeAvslutteBegrunnelse) {
                null -> KunneAvsluttes(avregistrering, erIservIArena, input)
                else -> KunneIkkeAvsluttes(avregistrering, erIservIArena, kunneIkkeAvslutteBegrunnelse, input)
            }
        }

        private fun kanAvsluttesIntern(input: KanAvsluttesInput, avregistreringsType: AvregistreringsType): List<KanIkkeAvslutteBegrunnelse> {
            /* Admin kan avslutte alt */
            if (avregistreringsType == AvregistreringsType.AdminAvregistrering) return emptyList()

            return listOfNotNull(
                if (!input.erUnderOppfolging) KanIkkeAvslutteBegrunnelse.BRUKER_VAR_IKKE_UNDER_OPPFOLGING else null,
                if (input.underKvp) KanIkkeAvslutteBegrunnelse.BRUKER_VAR_UNDER_KVP else null,
                if (input.harAktiveTiltaksdeltakelser) KanIkkeAvslutteBegrunnelse.BRUKER_HAR_AKTIVE_TILTAKSDELTAKELSER else null,
                if (input.erDeltakerIUngdomsprogrammet) KanIkkeAvslutteBegrunnelse.BRUKER_ER_DELTAKER_I_UNGDOMSPROGRAMMET else null,
                if (input.erArbeidssoeker) KanIkkeAvslutteBegrunnelse.BRUKER_ER_REGISTRERT_SOM_ARBEIDSSOKER else null,
                if (input.harAap) KanIkkeAvslutteBegrunnelse.BRUKER_MOTTAR_ELLER_HAR_SOKT_OM_AAP else null,
                if (!avregistreringsType.erManuellAvregistrering() && input.erOppfolgingForlenget) KanIkkeAvslutteBegrunnelse.OPPFOLGINGEN_ER_FORLENGET else null
            )
        }
    }
}

sealed interface AvslutningsInput {
    val avregistrering: Avregistrering
}

class KunneAvsluttesOverstyring(
    override val avregistrering: AdminAvregistrering
): AvslutningsInput

class KunneAvsluttes(
    override val avregistrering: Avregistrering,
    val erIserv: Boolean,
    kanAvsluttesInput: KanAvsluttesInput,
): KunneAvsluttesResultat(kanAvsluttesInput), AvslutningsInput

class KunneIkkeAvsluttes(
    val avregistrering: Avregistrering,
    val erIserv: Boolean,
    val begrunnelse: List<KanIkkeAvslutteBegrunnelse> = emptyList(),
    kanAvsluttesInput: KanAvsluttesInput,
): KunneAvsluttesResultat(kanAvsluttesInput)

enum class KanIkkeAvslutteBegrunnelse(val begrunnelse: String) {
    BRUKER_VAR_IKKE_UNDER_OPPFOLGING("bruker var ikke under oppfølging"),
    BRUKER_HAR_AKTIVE_TILTAKSDELTAKELSER("bruker hadde aktive tiltaksdeltakelser"),
    BRUKER_ER_DELTAKER_I_UNGDOMSPROGRAMMET("bruker er deltaker i ungdomsprogrammet"),
    BRUKER_ER_REGISTRERT_SOM_ARBEIDSSOKER("bruker er registrert som arbeidssøker"),
    BRUKER_MOTTAR_ELLER_HAR_SOKT_OM_AAP("bruker mottar eller har søkt om AAP"),
    OPPFOLGINGEN_ER_FORLENGET("oppfølgingen er forlenget"),
    BRUKER_VAR_UNDER_KVP("bruker var under kvp"),
}
