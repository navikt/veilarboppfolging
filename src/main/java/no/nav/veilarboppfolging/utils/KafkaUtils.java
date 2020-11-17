package no.nav.veilarboppfolging.utils;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

public class KafkaUtils {

    public static String requireKafkaTopicPrefix() {
        return isProduction().orElseThrow() ? "p" : "q1";
    }

}
