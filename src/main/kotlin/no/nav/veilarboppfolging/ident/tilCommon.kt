package no.nav.veilarboppfolging.ident

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr

fun String.toCommonIdent(onInvalid: () -> Nothing): EksternBrukerId {
    return when (val result = Ident.validate(this)) {
        is InvalidIdent -> onInvalid()
        is ValidIdent -> {
            when (result.ident) {
                is no.nav.veilarboppfolging.ident.AktorId -> AktorId(result.ident.value)
                is Dnr -> Fnr(result.ident.value)
                is no.nav.veilarboppfolging.ident.Fnr -> Fnr(result.ident.value)
                is Npid -> Fnr(result.ident.value)
            }
        }
    }
}