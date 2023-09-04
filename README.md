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


