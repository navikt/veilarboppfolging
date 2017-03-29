Applikasjonen kjører på Embedded Jetty lokalt. Den lokale configen legges i ApplicationMockConfig.
Spring REST er brukt som rest-API.

Applikasjonen gjør kall mot Arena og derfor så kan den kun kjøres mot mock lokalt.

#### domenebrukernavn/domenepassord
Applikasjonen er avhengig av variabler fra Fasit. I miljø går dette av seg selv, men lokalt må man autentisere seg selv
mot Fasit for å få tilgang til disse. Dette gjøres ved å legge inn brukernavn/passord til Fasit som miljøvariabler
på operativsystemet. Disse må ha navnene: domenebrukernavn/domenepassord. Intellij må gjennom en manuell restart for å
klare å plukke opp disse. File --> Invalidate Caches / Restart --> Just Restart er ikke tilstrekkelig. Må krysses ut
og startes på nytt.

På veilarbsituasjon/v2/api-docs så ligger en json som beskriver rest-apiet.
For å se en mer leslig dokumentasjon av rest-api gå på URL: veilarbsituasjon/swagger-ui.html.
F eks https://app-t4.adeo.no/veilarbsituasjon/swagger-ui.html
