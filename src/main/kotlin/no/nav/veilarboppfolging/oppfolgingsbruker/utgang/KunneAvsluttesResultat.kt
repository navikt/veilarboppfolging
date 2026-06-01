package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

data class KanAvsluttesInput(
    val erUnderOppfolging: Boolean,
    val erIservIArena: Boolean,
    val harAktiveTiltaksdeltakelser: Boolean,
    val erDeltakerIUngdomsprogrammet: Boolean,
    val erArbeidssoeker: Boolean,
    val harAap: Boolean,
    val underKvp: Boolean
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

        private fun kanAvsluttesIntern(input: KanAvsluttesInput, avregistreringsType: AvregistreringsType): String? {
            /* Admin kan avslutte alt */
            if (avregistreringsType == AvregistreringsType.AdminAvregistrering) return null

            if (!input.erUnderOppfolging) return "bruker var ikke under oppfølging"
            if (!avregistreringsType.erManuellAvregistrering() && !input.erIservIArena) return "bruker var ikke inaktivert i Arena ved forsøk på automatisk avslutning"
            if (input.underKvp) return "bruker var under kvp"
            if (input.harAktiveTiltaksdeltakelser) return "bruker hadde aktive tiltaksdeltakelser"
            if (input.erDeltakerIUngdomsprogrammet) return "bruker er deltaker i ungdomsprogrammet"
            if (input.erArbeidssoeker) return "bruker er registrert som arbeidssøker"
            if (input.harAap) return  "bruker har AAP"
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
    val begrunnelse: String? = null,
    kanAvsluttesInput: KanAvsluttesInput,
): KunneAvsluttesResultat(kanAvsluttesInput)

