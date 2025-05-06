package no.nav

data class HttpErrorResponseDto(
    val httpStatus: Int,
    val errorTitle: String,
    val errorMessage: String,
    val errorCode: String,
)