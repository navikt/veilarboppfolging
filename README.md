[![CircleCI](https://circleci.com/gh/navikt/veilarboppfolging.svg?style=svg)](https://circleci.com/gh/navikt/veilarboppfolging)

# Veilarboppfolging
Applikasjonen kjører på Embedded Jetty lokalt. Den lokale configen legges i ApplicationMockConfig.

#### domenebrukernavn/domenepassord
Applikasjonen er avhengig av variabler fra Fasit. I miljø går dette av seg selv, men lokalt må man autentisere seg selv
mot Fasit for å få tilgang til disse. Dette gjøres ved å legge inn brukernavn/passord til Fasit som miljøvariabler
på operativsystemet. Disse må ha navnene: domenebrukernavn/domenepassord. Intellij må gjennom en manuell restart for å
klare å plukke opp disse. File --> Invalidate Caches / Restart --> Just Restart er ikke tilstrekkelig. Må krysses ut
og startes på nytt.

#### Sending av veiledertilordninger til Portefølje
Riktig databaselink, brukernavn og passord må oppgis i metoden ``setupJndiLocalContext()`` i klassen
``JndiLocalContextConfig``. Gå til Fasit --> søk på veilarboppfolgingDB --> velg riktig miljø.

Kjør ``https://localhost:8485/veilarboppfolging/api/sendalleveiledertilordninger`` i nettleseren.

*OBS OBS!!* Husk at veilarbportefolje og veilarbportefoljeindeks må kjøre samtidig.

#### For å kjøre Webservicen (soap)
``veilarboppfolging-vilkar`` må klones ned til samme mappe som ``veilarboppfolging`` ligger i. I tillegg må 
``mvn clean install`` kjøres fra roten.
