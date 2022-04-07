package no.nav.veilarboppfolging.utils;

public class DownstreamApi {

    public final String cluster;
    public final String namespace;
    public final String serviceName;

    public DownstreamApi(String cluster, String namespace, String serviceName) {
        this.cluster = cluster;
        this.namespace = namespace;
        this.serviceName = serviceName;
    }
}
