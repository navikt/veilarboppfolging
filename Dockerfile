FROM docker.adeo.no:5000/bekkci/maven-builder
ADD / /source

# brukes av testene
ARG testmiljo
ARG domenebrukernavn
ARG domenepassord

RUN build


# TODO oppsett for nais
