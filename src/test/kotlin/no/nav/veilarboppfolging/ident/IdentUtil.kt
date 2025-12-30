package no.nav.veilarboppfolging.ident

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.AktorId

fun randomFnr(): Fnr {
    val date = (1..31).random().toString().padStart(2, '0')
    val month = (1..12).random().toString().padStart(2, '0')
    val year = (1..99).random().toString().padStart(2, '0')
    val randomDigits = (1..5).map { (0..9).random() }.joinToString("")
    return Fnr("${date}${month}${year}${randomDigits}")
}

//fun randomDnr(): Dnr {
//    val date = (1..31).random().toString().padStart(2, '0')
//        .replaceFirstChar { firstDigit -> firstDigit.plus(4) }
//    val month = (1..12).random().toString().padStart(2, '0')
//    val year = (1..99).random().toString().padStart(2, '0')
//    val randomDigits = (1..5).map { (0..9).random() }.joinToString("")
//    return Dnr("${date}${month}${year}${randomDigits}")
//}

fun randomAktorId(): AktorId {
    val randomDigits = (1..13).map { (0..9).random() }.joinToString("")
    return AktorId("$randomDigits")
}