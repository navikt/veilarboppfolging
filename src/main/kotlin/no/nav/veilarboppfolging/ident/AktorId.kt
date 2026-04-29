package no.nav.veilarboppfolging.ident

class AktorId(override val value: String): Ident() {
    init {
        require(value.isNotBlank()) { "AktorId cannot be blank" }
        require(value.length == 13) { "AktorId must be 13 characters long but was ${value.length}" }
        require(value.all { it.isDigit() }) { "AktorId must contain only digits" }
    }
}