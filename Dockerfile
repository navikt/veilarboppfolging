FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25

ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/install/*/lib /lib
EXPOSE 8080

USER nonroot
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.veilarboppfolging.Application"]
