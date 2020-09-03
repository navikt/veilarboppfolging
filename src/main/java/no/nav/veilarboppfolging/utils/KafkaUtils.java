package no.nav.veilarboppfolging.utils;

import static no.nav.common.utils.EnvironmentUtils.requireNamespace;

public class KafkaUtils {

    public static String requireKafkaTopicPrefix() {
        String namespace = requireNamespace();
        return namespace.equals("default") ? "p" : namespace;
    }

}
