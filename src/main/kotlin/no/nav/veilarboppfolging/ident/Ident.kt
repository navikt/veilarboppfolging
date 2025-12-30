package no.nav.veilarboppfolging.ident

sealed class Ident {
    abstract val value: String

    companion object {
        val isDev = System.getenv("NAIS_CLUSTER_NAME")?.contains("dev") ?: false

        fun validate(value: String): MaybeValidIdent {
            if (value.isBlank()) return InvalidIdent(value, "Ident cannot be blank")
            if (value.any { !it.isDigit() }) return InvalidIdent(value,"Ident must contain only digits")
            val length = value.length
            if (length != 11 && length != 13) return InvalidIdent(value,"Ident must have length 11 or 13 but had $length")

            val digitNumber3and4 by lazy { value.substring(2,4).toInt() }
            val firstDigit by lazy { value[0].digitToInt() }
            val lengthIs13 by lazy { length == 13 }
            val monthIsValidMonth by lazy { digitNumber3and4 in 1..12 }
            val monthIsTenorMonth by lazy { digitNumber3and4 in 81..92 }
            val monthIsDollyMonth by lazy { digitNumber3and4 in 41..80 }
            val monthIsBostMonth by lazy { digitNumber3and4 in 61..72 }
            val lengthIs11 by lazy { length == 11 }
            val isValidDate by lazy { value.take(2).toInt() in 1..31 }

            return when {
                lengthIs13 -> AktorId(value)
                firstDigit in gyldigeDnrStart && (monthIsValidMonth || monthIsTenorMonth || monthIsDollyMonth) -> Dnr(
                    value
                )
                digitNumber3and4 in 21..32 -> Npid(value) // NPID er måned + 20
                lengthIs11 && monthIsValidMonth && isValidDate -> Fnr(value)
                isDev && lengthIs11 && isValidDate && (monthIsTenorMonth || monthIsDollyMonth || monthIsBostMonth) -> Fnr(
                    value,
                )
                else -> return InvalidIdent(value)
            }.let { ValidIdent(it) }
        }

        fun validateOrThrow(value: String): Ident {
            return when (val res = validate(value)) {
                is InvalidIdent -> throw Exception(res.message)
                is ValidIdent -> res.ident
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Ident) return false
        return other.value == value
    }

    override fun toString() = value

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

sealed class MaybeValidIdent(val value: String)
class ValidIdent(val ident: Ident): MaybeValidIdent(ident.value)
class InvalidIdent(value: String, val message: String = "Ugyldig ident") : MaybeValidIdent(value)
