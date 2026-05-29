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

sealed class KunneAvsluttesResultat {
    companion object {
        @JvmStatic
        fun kanAvsluttes(
            avregistrering: Avregistrering,
            input: KanAvsluttesInput,
        ): KunneAvsluttesResultat {
            val avregistreringsType = avregistrering.getAvregistreringsType()
            val erIservIArena = input.erIservIArena
            val kunneIkkeAvslutteBegrunnelse = kanAvsluttesIntern(input, avregistreringsType)
            return when (kunneIkkeAvslutteBegrunnelse) {
                null -> KunneAvsluttes(avregistrering, erIservIArena)
                else -> avregistrering.nei(erIservIArena, kunneIkkeAvslutteBegrunnelse)
            }
        }

        @JvmStatic
        fun kanKanAvsluttesManuelt(input: KanAvsluttesInput): Boolean {
            return kanAvsluttesIntern(input, AvregistreringsType.ManuellAvregistrering) == null
        }

        fun kanAvsluttesPgaIservIArena(input: KanAvsluttesInput, kanReaktiveres: Boolean): String? {
            return when (kanReaktiveres) {
                false -> kanAvsluttesIntern(input, AvregistreringsType.ArenaIservKanIkkeReaktiveres)
                true -> "Bruker kan enkelt reaktiveres i Arena, og vil derfor ikke automatisk avsluttes pga inaktivering"
            }
        }

        private fun kanAvsluttesIntern(input: KanAvsluttesInput, avregistreringsType: AvregistreringsType): String? {
            /* Admin kan avslutte alt */
            if (avregistreringsType == AvregistreringsType.AdminAvregistrering) return null

            val erIservIArena = input.erIservIArena
            if (!input.erUnderOppfolging) return "bruker var ikke under oppfølging"
            if (!avregistreringsType.erManuellAvregistrering() && !erIservIArena) return "bruker var ikke inaktivert i Arena ved forsøk på automatisk avslutning"
            if (input.underKvp) return "bruker var under kvp"
            if (input.harAktiveTiltaksdeltakelser) return "bruker hadde aktive tiltaksdeltakelser"
            if (input.erDeltakerIUngdomsprogrammet) return "bruker er deltaker i ungdomsprogrammet"
            if (input.erArbeidssoeker) return "bruker er registrert som arbeidssøker"
            if (input.harAap) return  "bruker har AAP"
            return null
        }
        private fun Avregistrering.nei(iserv: Boolean, begrunnelse: String) = KunneIkkeAvsluttes( this,iserv, begrunnelse)
    }
}

class KunneAvsluttes(
    val avregistrering: Avregistrering,
    var erIserv: Boolean
): KunneAvsluttesResultat()

class KunneIkkeAvsluttes(
    val avregistrering: Avregistrering,
    var erIserv: Boolean,
    var begrunnelse: String? = null
): KunneAvsluttesResultat()

