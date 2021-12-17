# Veilarboppfolging
Tjeneste som lagrer informasjon om status for arbeidsrettet oppfølging for en bruker.

## Kafka
### Topic for endring på oppfølgingstatus
Ved endring på oppfølgingstatus for en bruker publiseres det en melding på følgende topic: 

prod-fss:
- `aapen-fo-endringPaaOppfolgingStatus-v1-p`

dev-fss:
- `aapen-fo-endringPaaOppfolgingStatus-v1-q0`
- `aapen-fo-endringPaaOppfolgingStatus-v1-q1`

Denne topicen er konfigurert med log compaction på aktørID, og inneholder historikk for alle brukere under arbeidsrettet oppfølging.

Eksempel på melding:

```json
{
  "aktoerid": "00000000000",
  "veileder": "Z000000",
  "oppfolging": true,
  "nyForVeileder": false,
  "manuell": false,
  "endretTimestamp": "2020-05-01T14:54:02.13+02:00",
  "startDato": "2020-01-01T14:54:02.13+01:00"
}
```

Meldingen inkluderer også en header `Nav-Call-Id` som kan benyttes som korrelasjonsID.

### Hvordan oppdatere en topic
Se beskrivelse av hvordan man kan oppdaterer topics i https://github.com/navikt/pto-config (privat repo)

## Interne endepunkter

| Endepunkt                                               | Beskrivelse                                                        |      
| --------------------------------------------------------| -------------------------------------------------------------------|
| `/internal/publiser_oppfolging_status_historikk`           | Legg ut *alle* brukere på topic for endring av oppfølgingstatus  |
| `/internal/publiser_oppfolging_status?aktoerId=<aktoerId>` | Publiser oppfølgingstatus på nytt for gjeldende bruker           |
