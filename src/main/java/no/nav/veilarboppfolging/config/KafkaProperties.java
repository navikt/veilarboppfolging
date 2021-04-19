package no.nav.veilarboppfolging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    String brokersUrl;
    String endringPaaOppfolgingBruker;
    String endringPaaAvsluttOppfolging; // Deprecated, erstattes av 'oppfolgingAvsluttet'
    String oppfolgingStartet;
    String oppfolgingAvsluttet;
    String kvpStartet;
    String kvpAvlsuttet;
    String endringPaManuellStatus;
    String veilederTilordnet;
    String endringPaNyForVeileder;
    String endringPaMal;
}
