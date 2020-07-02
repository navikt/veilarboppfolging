package no.nav.veilarboppfolging.feed.cjm.util;

import no.nav.common.utils.EnvironmentUtils;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class UrlUtils {

    public final static String QUERY_PARAM_PAGE_SIZE = "page_size";
    public final static String QUERY_PARAM_ID = "id";

    private static Pattern pattern = Pattern.compile("([^:]\\/)\\/+");

    public static String callbackUrl(String root, String feedname) {
        return asUrl(getHost(), root, "feed", feedname);
    }

    private static String getHost() {
        return EnvironmentUtils.isProduction().orElse(false)
                ? "https://app.adeo.no"
                : format("https://app-%s.adeo.no", EnvironmentUtils.requireNamespace());
    }

    public static String asUrl(String... s) {
        String url = Stream.of(s).collect(Collectors.joining("/"));
        return pattern.matcher(url).replaceAll("$1");
    }
}
