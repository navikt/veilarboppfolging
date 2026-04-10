package no.nav.veilarboppfolging.ident

/*
* Kan innholde fnr, dnr eller npid
* */
class Fnr(override val value: String): Ident() {
    init {
        require(value.isNotBlank()) { "Fnr cannot be blank" }
        require(value.length == 11) { "Fnr $value must be 11 characters long but was ${value.length}" }
        require(value.all { it.isDigit() }) { "Fnr must contain only digits" }
    }

    override fun toString(): String = value
}