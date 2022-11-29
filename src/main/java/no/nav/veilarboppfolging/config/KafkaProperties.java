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
    String endringPaaAvsluttOppfolgingTopic; // Deprecated, erstattes av 'oppfolgingAvsluttet'
    String oppfolgingStartetTopic;
    String oppfolgingAvsluttetTopic;
    String kvpStartetTopic;
    String kvpAvsluttetTopic;
    String endringPaManuellStatusTopic;
    String veilederTilordnetTopic;
    String endringPaNyForVeilederTopic;
    String sisteOppfolgingsperiodeTopic;
    String oppfolgingsperiodeTopic;
    String sisteTilordnetVeilederTopic;
    String endringPaMalAiven;
    String kvpAvsluttetTopicAiven;
}
