FROM ghcr.io/navikt/baseimages/temurin:21

COPY /target/veilarboppfolging.jar app.jar
