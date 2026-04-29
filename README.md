# Veilarboppfolging
Tjeneste som lagrer informasjon om status for arbeidsrettet oppfølging for en bruker.

## Kafka
### Aiven topics det publiseres til
pto.siste-oppfolgingsperiode-v1
pto.oppfolgingsperiode-v1
pto.siste-tilordnet-veileder-v1
pto.veileder-tilordnet-v1
pto.endring-paa-manuell-status-v1
pto.endring-paa-ny-for-veileder-v1
pto.endring-paa-maal-v1
pto.kvp-avsluttet-v1
pto.kvp-startet-v1

### Aiven topics som konsumeres
pto.endring-paa-oppfolgingsbruker-v2

Eksempel på melding: Se repo pto-schema

Meldingen inkluderer en header `Nav-Call-Id` som kan benyttes som korrelasjonsID.

### Hvordan oppdatere en topic
Se beskrivelse av hvordan man kan oppdatere topics i https://github.com/navikt/pto-config (privat repo)

## Interne endepunkter

| Endepunkt                                               | Beskrivelse                                                        |      
| --------------------------------------------------------| -------------------------------------------------------------------|
| `/internal/publiser_oppfolging_status_historikk`           | Legg ut *alle* brukere på topic for endring av oppfølgingstatus  |
| `/internal/publiser_oppfolging_status?aktoerId=<aktoerId>` | Publiser oppfølgingstatus på nytt for gjeldende bruker           |

## Avro-genererte klasser
Avro-genererte klasser er lagt inn manuelt og er ikke en del av pipelinen. Hvis skjemaversjonen skal oppdateres må det genereres nye klasser. 

## Kode generert av GitHub Copilot

Dette repoet bruker GitHub Copilot til å generere kode.

### Teste i GraphiQL playground i dev
- Skaff et OBO token fra nais sin [azure-token-generator](https://azure-token-generator.intern.dev.nav.no/api/obo?aud=dev-gcp.poao.veilarboppfolging)
- Bruk token som en "Authorization" header i `Headers` fanen nederst i [GraphiQl](https://veilarboppfolging.intern.dev.nav.no/veilarboppfolging/graphiql)
    - `{ "Authorization": "Bearer <token>", "nav-consumer-id": "graphiql" }`
    - Sett `{ "fnr": "<fnr eller aktorId>" }` i variables
- Profit?