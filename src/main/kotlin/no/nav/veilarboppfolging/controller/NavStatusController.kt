package no.nav.veilarboppfolging.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/navstatus")
class NavStatusController(
    ) {

    @GetMapping
    fun okStatus(): NavStatusDto {

        return NavStatusDto(
            NavStatus.OK,
            description = "NAV OK")
    }

    enum class NavStatus {OK, ISSUE, DOWN}
    data class NavStatusDto(
        val status: NavStatus,
        val description: String,
        val logLink: String = "https://logs.adeo.no/app/r/s/ZU3Pq"
    ) {
    }
}