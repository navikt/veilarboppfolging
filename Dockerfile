FROM docker.pkg.github.com/navikt/pus-nais-java-app/pus-nais-java-app:java8
COPY /target/veilarboppfolging /app
