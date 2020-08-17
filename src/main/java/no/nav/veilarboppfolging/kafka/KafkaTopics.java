package no.nav.veilarboppfolging.kafka;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaTopics {

    private String endringPaaOppfolgingBruker;

    private String endringPaaAvsluttOppfolging;

    private String endringPaaOppfolgingStatus;

    private String endringPaKvp;

    private String oppfolgingStartet;

    public static KafkaTopics create(String topicPrefix) {
        KafkaTopics kafkaTopics = new KafkaTopics();
        kafkaTopics.setEndringPaaOppfolgingBruker("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + topicPrefix);
        kafkaTopics.setEndringPaaAvsluttOppfolging("aapen-fo-endringPaaAvsluttOppfolging-v1-" + topicPrefix);
        kafkaTopics.setEndringPaaOppfolgingStatus("aapen-fo-endringPaaOppfolgingStatus-v1-" + topicPrefix);
        kafkaTopics.setEndringPaKvp("aapen-arbeidsrettetOppfolging-endringPaKvp-v1-" + topicPrefix);
        kafkaTopics.setOppfolgingStartet("aapen-arbeidsrettetOppfolging-oppfolgingStartet-v1-" + topicPrefix);
        return kafkaTopics;
    }

    public String[] getAllTopics() {
        return new String[] {
                this.getEndringPaaOppfolgingBruker(),
                this.getEndringPaaAvsluttOppfolging(),
                this.getEndringPaaOppfolgingStatus()
        };
    }

}
