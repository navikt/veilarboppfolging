package no.nav.veilarboppfolging.ident

val gyldigeDnrStart = listOf(4,5,6,7)
class Dnr(override val value: String): Ident() {
    init {
        require(value.isNotBlank()) { "Dnr cannot be blank" }
        require(value.length == 11) { "Dnr $value must be 11 characters long but was ${value.length}" }
        require(value.all { it.isDigit() }) { "Dnr must contain only digits" }
        require(gyldigeDnrStart.contains(value[0].digitToInt()) ) { "Dnr must start with 4, 5, 6, or 7" }
    }

    override fun toString(): String = value
}