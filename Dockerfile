FROM gcr.io/distroless/java21

ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/veilarboppfolging.jar ./
EXPOSE 8080
USER nonroot
CMD ["veilarboppfolging.jar"]
