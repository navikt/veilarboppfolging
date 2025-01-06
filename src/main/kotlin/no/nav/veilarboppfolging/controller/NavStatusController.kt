package no.nav.veilarboppfolging.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/internal/navstatus")
class NavStatusController(
    ) {

    @GetMapping("/")
    fun okStatus(): NavStatusDto {

        return NavStatusDto(
            NAV_STATUS.OK,
            description = "NAV OK")
    }

    enum class NAV_STATUS {OK, ISSUE, DOWN}
    data class NavStatusDto(
        val status: NAV_STATUS,
        val description: String,
        val logLink: String = "https://logs.adeo.no/app/r/s/ZU3Pq"
    ) {
    }
}