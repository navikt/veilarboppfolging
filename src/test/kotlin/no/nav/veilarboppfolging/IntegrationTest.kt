package no.nav.veilarboppfolging

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nimbusds.jwt.JWTClaimsSet
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.norg2.Enhet
import no.nav.common.client.norg2.Norg2Client
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.poao_tilgang.api.dto.response.Diskresjonskode
import no.nav.poao_tilgang.api.dto.response.TilgangsattributterResponse
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.NavAnsattTilgangTilNavEnhetPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.poao_tilgang.client.api.ApiResult
import no.nav.poao_tilgang.client.api.NetworkApiException
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.tms.varsel.builder.BuilderEnvironment
import no.nav.veilarboppfolging.client.norg.INorgTilhorighetClient
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.client.pdl.GTType
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningClient
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningNr
import no.nav.veilarboppfolging.client.pdl.PdlFolkeregisterStatusClient
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsBruker
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolgingsStatus
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.config.EnvironmentProperties
import no.nav.veilarboppfolging.config.KafkaProperties
import no.nav.veilarboppfolging.controller.OppfolgingController
import no.nav.veilarboppfolging.controller.SakController
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.VeilederRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.AktiverBrukerManueltService
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ManuellAvregistrering
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.OppfolgingsHendelseDto
import no.nav.veilarboppfolging.repository.EnhetRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.SakRepository
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.MetricsService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import no.nav.veilarboppfolging.test.DbTestUtils
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdMachineToMachineTokenClient
import no.nav.veilarboppfolging.tokenClient.ErrorMappedAzureAdOnBehalfOfTokenClient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

@EmbeddedKafka(partitions = 1)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
open class IntegrationTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeClass() {
            BuilderEnvironment.extend(mapOf(
                "NAIS_APP_NAME" to "some-value",
                "NAIS_NAMESPACE" to "some-value",
                "NAIS_CLUSTER_NAME" to "some-value"
            ))
        }
    }

    @MockitoBean
    private lateinit var veilarbarenaClient: VeilarbarenaClient

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @MockitoBean
    lateinit var authContextHolder: AuthContextHolder

    @MockitoBean
    lateinit var azureAdOnBehalfOfTokenClient: ErrorMappedAzureAdOnBehalfOfTokenClient

    @MockitoBean
    lateinit var norg2Client: Norg2Client

    @MockitoBean
    lateinit var geografiskTilknytningClient: GeografiskTilknytningClient

    @MockitoBean
    lateinit var pdlFolkeregisterStatusClient: PdlFolkeregisterStatusClient

    @MockitoBean
    lateinit var environmentProperties: EnvironmentProperties

    @MockitoBean
    lateinit var metricsService: MetricsService

    @Autowired
    lateinit var aktorOppslagClient: AktorOppslagClient

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var oppfolgingController: OppfolgingController

    @Autowired
    lateinit var oppfolgingService: OppfolgingService

    @Autowired
    lateinit var startOppfolgingService: StartOppfolgingService

    @Autowired
    lateinit var oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository

    @Autowired
    lateinit var sakController: SakController

    @Autowired
    lateinit var arenaOppfolgingService: ArenaOppfolgingService

    @Autowired
    lateinit var oppfolgingsStatusRepository: OppfolgingsStatusRepository

    @MockitoBean
    lateinit var azureMachineToMachineTokenClient: ErrorMappedAzureAdMachineToMachineTokenClient

    @Autowired
    lateinit var sakRepository: SakRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var enhetRepository: EnhetRepository

    @Autowired
    lateinit var poaoTilgangClient: PoaoTilgangClient

    @Autowired
    lateinit var inorg: INorgTilhorighetClient

    @Autowired
    lateinit var aktiverBrukerManueltService: AktiverBrukerManueltService

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    @Autowired
    lateinit var template: NamedParameterJdbcTemplate

    @BeforeEach
    fun beforeEach() {
        DbTestUtils.cleanupTestDb(jdbcTemplate)
    }

    fun startOppfolgingSomArbeidsoker(aktørId: AktorId, fnr: Fnr) {
        val bruker = OppfolgingsRegistrering.arbeidssokerRegistrering(fnr, aktørId, BrukerRegistrant(fnr))
        startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(bruker)
    }

    fun setBrukerUnderOppfolging(aktorId: AktorId, fnr: Fnr) {
        val bruker = OppfolgingsRegistrering.arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(bruker)
    }

    fun hentOppfolgingsperioder(fnr: Fnr) = oppfolgingController.hentOppfolgingsperioder(fnr)

    fun avsluttOppfolgingManueltSomVeileder(aktorId: AktorId, veileder: String = "veileder", begrunnelse: String = "Begrunnelse") {
        oppfolgingService.avsluttOppfolging(
            ManuellAvregistrering(aktorId, VeilederRegistrant(NavIdent.of(veileder)), begrunnelse),
        )
    }

    fun mockSytemBrukerAuthOk(aktørId: AktorId, fnr: Fnr) {
        val claims = JWTClaimsSet.Builder()
            .issuer("microsoftonline.com")
            .claim("azp_name", "cluster:team:veilarbregistrering")
            .claim("roles", listOf("access_as_application"))
            .build()

        `when`(authContextHolder.idTokenClaims).thenReturn(Optional.of(claims))

        val token = "token"

        `when`(authContextHolder.idTokenString).thenReturn(Optional.of(token))

        `when`(authContextHolder.erSystemBruker()).thenReturn(true)
        `when`(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktørId)
        `when`(aktorOppslagClient.hentFnr(aktørId)).thenReturn(fnr)
    }

    fun mockInternBrukerAuthOk(veilederIOD: UUID,aktørId: AktorId, fnr: Fnr, navIdent: String = "A123456") {
        val claims = JWTClaimsSet.Builder()
            .issuer("microsoftonline.com")
            .claim("azp_name", "cluster:team:veilarbregistrering")
            .claim("oid", veilederIOD.toString())
            .build()

        `when`(authContextHolder.idTokenClaims).thenReturn(Optional.of(claims))

        val token = "token"

        `when`(authContextHolder.idTokenString).thenReturn(Optional.of(token))

        `when`(authContextHolder.getRole()).thenReturn(Optional.of(UserRole.INTERN))
//        `when`(authContextHolder).thenReturn(Optional.of(UserRole.INTERN))
        `when`(authContextHolder.erInternBruker()).thenReturn(true)
        `when`(authContextHolder.erEksternBruker()).thenReturn(false)
        `when`(aktorOppslagClient.hentAktorId(fnr)).thenReturn(aktørId)
        `when`(aktorOppslagClient.hentFnr(aktørId)).thenReturn(fnr)
        `when`(authContextHolder.uid).thenReturn(Optional.of(navIdent))
    }

    fun mockPdlGeografiskTilknytning(fnr: Fnr, enhetsNr: String, gtType: GTType = GTType.BYDEL) {
        `when`(geografiskTilknytningClient.hentGeografiskTilknytning(fnr))
            .thenReturn(GeografiskTilknytningClient.GeografiskTilknytningOgAdressebeskyttelse(
                GeografiskTilknytningNr(gtType, enhetsNr),
                false)
            )
    }

    fun mockPdlFolkeregisterStatus(fnr: Fnr, status: FregStatusOgStatsborgerskap) {
        `when`(pdlFolkeregisterStatusClient.hentFolkeregisterStatus(fnr)).thenReturn(status)
    }

    fun mockPoaoTilgangTilgangsAttributter(kontor: String, skjermet: Boolean, diskresjonskode: Diskresjonskode? = null) {
        val apiResult = ApiResult.success(TilgangsattributterResponse(
            kontor = kontor,
            skjermet = skjermet,
            diskresjonskode = diskresjonskode
        ))
        doReturn(apiResult).`when`(poaoTilgangClient).hentTilgangsAttributter(anyString())
    }

    fun mockPoaoTilgangTilgangsAttributterFeiler() {
        val apiResult = ApiResult.failure<NetworkApiException>(NetworkApiException(IllegalArgumentException(")")))
        doReturn(apiResult).`when`(poaoTilgangClient).hentTilgangsAttributter(anyString())
    }

    fun mockPoaoTilgangHarTilgangTilBruker(veilederUuid: UUID, fnr: Fnr, decision: Decision, tilgangType: TilgangType = TilgangType.LESE) {
        val policyInput = NavAnsattTilgangTilEksternBrukerPolicyInput(
            navAnsattAzureId = veilederUuid,
            tilgangType = tilgangType,
            norskIdent = fnr.get()
        )
        val apiResult = ApiResult.success(decision)
        doReturn(apiResult).`when`(poaoTilgangClient).evaluatePolicy(policyInput)
    }

    fun mockPoaoTilgangHarTilgangTilEnhet(veilederUuid: UUID, enhetId: EnhetId) {
        val policyInput = NavAnsattTilgangTilNavEnhetPolicyInput(veilederUuid, enhetId.get())
        doReturn(ApiResult.success(Decision.Permit)).`when`(poaoTilgangClient).evaluatePolicy(policyInput)
    }

    fun mockNorgEnhetsNavn(enhetsNr: String, enhetsNavn: String) {
        val enhet = Enhet().also { it.navn = enhetsNavn }
        `when`(norg2Client.hentEnhet(enhetsNr)).thenReturn(enhet)
    }

    fun mockVeilarbArenaOppfolgingsStatus(fnr: Fnr, formidlingsgruppe: Formidlingsgruppe? = Formidlingsgruppe.ARBS, kanEnkeltReaktiveres: Boolean? = false, serviceGruppe: String? = "IVURD", oppfolgingsEnhet: String? = "1234", inaktiveringsDato: LocalDate? = null) {
        `when`(veilarbarenaClient.getArenaOppfolgingsstatus(fnr)).thenReturn(
            Optional.of(
                VeilarbArenaOppfolgingsStatus()
                    .setRettighetsgruppe(null)
                    .setFormidlingsgruppe(formidlingsgruppe?.name)
                    .setServicegruppe(serviceGruppe)
                    .setOppfolgingsenhet(oppfolgingsEnhet)
                    .setInaktiveringsdato(inaktiveringsDato)
                    .setKanEnkeltReaktiveres(kanEnkeltReaktiveres)
                )
            )
    }

    fun mockVeilarbArenaOppfolgingsBruker(fnr: Fnr, formidlingsgruppe: Formidlingsgruppe? = Formidlingsgruppe.ARBS, kvalifiseringsgruppe: Kvalifiseringsgruppe = Kvalifiseringsgruppe.BATT, oppfolgingsEnhet: String? = "1234") {
        `when`(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(
            Optional.of(
                VeilarbArenaOppfolgingsBruker()
                    .setFodselsnr(fnr.get())
                    .setFormidlingsgruppekode(formidlingsgruppe?.name)
                    .setHovedmaalkode("BEHOLDEA")
                    .setIserv_fra_dato(ZonedDateTime.now())
                    .setKvalifiseringsgruppekode(kvalifiseringsgruppe.name)
                    .setNav_kontor(oppfolgingsEnhet)
            )
        )
    }

    private val objectMapper = JsonUtils.getMapper().also {
        it.registerKotlinModule()
    }
    /* Kafka producer saves record to the kafka_producer_record table before publishing them to kafka */
    fun getRecordsStoredInKafkaOutbox(topic: String, fnr: String): List<OppfolgingsHendelseDto> {
        return template.query("""
            SELECT * FROM kafka_producer_record
            where topic = :topic and key = :fnr
        """.trimIndent(), mapOf("topic" to topic, "fnr" to fnr.toByteArray())) { resultSet, row ->
            val json = resultSet.getBytes("value").toString(Charsets.UTF_8)
            objectMapper.readValue(json, OppfolgingsHendelseDto::class.java)
        }
    }
}