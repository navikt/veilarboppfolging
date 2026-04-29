package no.nav.veilarboppfolging.ident

class Npid(override val value: String): Ident() {
    init {
        require(value.isNotBlank()) { "Npid cannot be blank" }
        require(value.length == 11) { "Npid must be 11 characters long but was ${value.length}" }
        require(value.all { it.isDigit() }) { "Npid must contain only digits" }
    }

    override fun toString(): String = value
}