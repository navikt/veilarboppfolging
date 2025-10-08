FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-24

ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/install/*/lib /lib
RUN /bin/mkdir /secure-logs
RUN chown nonroot /secure-logs
EXPOSE 8080

USER nonroot
ENTRYPOINT ["java", "-cp", "/lib/*", "no.nav.veilarboppfolging.Application"]
