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

        private fun kanAvsluttesIntern(input: KanAvsluttesInput, avregistreringsType: AvregistreringsType): AvslutningsBegrunnelse? {
            /* Admin kan avslutte alt */
            if (avregistreringsType == AvregistreringsType.AdminAvregistrering) return null

            if (!input.erUnderOppfolging) return AvslutningsBegrunnelse.BRUKER_VAR_IKKE_UNDER_OPPFOLGING
            if (input.underKvp) return AvslutningsBegrunnelse.BRUKER_VAR_UNDER_KVP
            if (input.harAktiveTiltaksdeltakelser) return AvslutningsBegrunnelse.BRUKER_HAR_AKTIVE_TILTAKSDELTAKELSER
            if (input.erDeltakerIUngdomsprogrammet) return AvslutningsBegrunnelse.BRUKER_ER_DELTAKER_I_UNGDOMSPROGRAMMET
            if (input.erArbeidssoeker) return AvslutningsBegrunnelse.BRUKER_ER_REGISTRERT_SOM_ARBEIDSSOKER
            if (input.harAap) return AvslutningsBegrunnelse.BRUKER_MOTTAR_ELLER_HAR_SOKT_OM_AAP
            if (!avregistreringsType.erManuellAvregistrering() && input.erOppfolgingForlenget) return AvslutningsBegrunnelse.OPPFOLGINGEN_ER_FORLENGET
            return null
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
    val begrunnelse: AvslutningsBegrunnelse? = null,
    kanAvsluttesInput: KanAvsluttesInput,
): KunneAvsluttesResultat(kanAvsluttesInput)

enum class AvslutningsBegrunnelse(val begrunnelse: String) {
    BRUKER_VAR_IKKE_UNDER_OPPFOLGING("bruker var ikke under oppfølging"),
    BRUKER_HAR_AKTIVE_TILTAKSDELTAKELSER("bruker hadde aktive tiltaksdeltakelser"),
    BRUKER_ER_DELTAKER_I_UNGDOMSPROGRAMMET("bruker er deltaker i ungdomsprogrammet"),
    BRUKER_ER_REGISTRERT_SOM_ARBEIDSSOKER("bruker er registrert som arbeidssøker"),
    BRUKER_MOTTAR_ELLER_HAR_SOKT_OM_AAP("bruker mottar eller har søkt om AAP"),
    OPPFOLGINGEN_ER_FORLENGET("oppfølgingen er forlenget"),
    BRUKER_VAR_UNDER_KVP("bruker var under kvp"),
}
