package no.nav.veilarboppfolging.kafka;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaTopics {

    private String endringPaaOppfolgingBruker;

    private String endringPaaAvsluttOppfolging; // Deprecated, erstattes av 'oppfolgingAvsluttet'

    private String oppfolgingStartet;

    private String oppfolgingAvsluttet;

    private String kvpStartet;

    private String kvpAvlsuttet;

    private String endringPaManuellStatus;

    private String veilederTilordnet;

    private String endringPaNyForVeileder;

    private String endringPaMal;

    public static KafkaTopics create(String topicPrefix) {
        KafkaTopics kafkaTopics = new KafkaTopics();

        // Consumer topics
        kafkaTopics.setEndringPaaOppfolgingBruker("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + topicPrefix);

        // Producer topics
        kafkaTopics.setEndringPaaAvsluttOppfolging("aapen-fo-endringPaaAvsluttOppfolging-v1-" + topicPrefix);
        kafkaTopics.setOppfolgingStartet("aapen-arbeidsrettetOppfolging-oppfolgingStartet-v1-" + topicPrefix);
        kafkaTopics.setOppfolgingAvsluttet("aapen-arbeidsrettetOppfolging-oppfolgingAvsluttet-v1-" + topicPrefix);
        kafkaTopics.setKvpStartet("aapen-arbeidsrettetOppfolging-kvpStartet-v1-" + topicPrefix);
        kafkaTopics.setKvpAvlsuttet("aapen-arbeidsrettetOppfolging-kvpAvsluttet-v1-" + topicPrefix);
        kafkaTopics.setEndringPaManuellStatus("aapen-arbeidsrettetOppfolging-endringPaManuellStatus-v1-" + topicPrefix);
        kafkaTopics.setVeilederTilordnet("aapen-arbeidsrettetOppfolging-veilederTilordnet-v1-" + topicPrefix);
        kafkaTopics.setEndringPaNyForVeileder("aapen-arbeidsrettetOppfolging-endringPaNyForVeileder-v1-" + topicPrefix);
        kafkaTopics.setEndringPaMal("aapen-arbeidsrettetOppfolging-endringPaMal-v1-" + topicPrefix);

        return kafkaTopics;
    }

    public String[] getAllTopics() {
        return new String[] {
                this.getEndringPaaOppfolgingBruker(),
                this.getEndringPaaAvsluttOppfolging(),
                this.getKvpStartet(),
                this.getKvpAvlsuttet(),
                this.getOppfolgingStartet(),
                this.getEndringPaManuellStatus(),
                this.getVeilederTilordnet(),
                this.getEndringPaNyForVeileder(),
        };
    }

}
