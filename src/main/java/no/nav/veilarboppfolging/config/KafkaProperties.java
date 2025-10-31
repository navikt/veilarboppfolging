package no.nav.veilarboppfolging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    String brokersUrl;
    String endringPaaOppfolgingBrukerTopic;
    String endringPaManuellStatusTopic;
    String veilederTilordnetTopic;
    String endringPaNyForVeilederTopic;
    String sisteOppfolgingsperiodeTopic;
    String sisteOppfolgingsperiodeTopicV2;
    String oppfolgingsperiodeTopic;
    String sisteTilordnetVeilederTopic;
    String endringPaMalAiven;
    String kvpAvsluttetTopicAiven;
    String kvpStartetTopicAiven;
    String kvpPerioderTopicAiven;
    String arbeidssokerperioderTopicAiven;
    String minSideAapenMicrofrontendV1;
    String minSideBrukerVarsel;
    String oppfolgingshendelseV1;
    String arbeidsoppfolgingskontortilordningTopic;
}
