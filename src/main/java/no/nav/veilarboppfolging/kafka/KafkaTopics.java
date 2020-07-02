package no.nav.veilarboppfolging.kafka;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaTopics {

    private String endringPaaOppfolgingBruker;

    private String endringPaaAvsluttOppfolging;

    private String endringPaaOppfolgingStatus;

    public static KafkaTopics create(String topicPrefix) {
        KafkaTopics kafkaTopics = new KafkaTopics();
        kafkaTopics.setEndringPaaOppfolgingBruker("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + topicPrefix);
        kafkaTopics.setEndringPaaAvsluttOppfolging("aapen-fo-endringPaaAvsluttOppfolging-v1-" + topicPrefix);
        kafkaTopics.setEndringPaaOppfolgingStatus("aapen-fo-endringPaaOppfolgingStatus-v1-" + topicPrefix);
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
