FROM docker.pkg.github.com/navikt/pus-nais-java-app/pus-nais-java-app:java11

COPY init.sh /init-scripts/init.sh
COPY /target/veilarboppfolging.jar app.jar