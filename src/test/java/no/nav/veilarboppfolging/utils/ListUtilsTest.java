package no.nav.veilarboppfolging.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ListUtilsTest {

    @Test
    public void firstOrNull__should_return_first_if_not_empty() {
        assertEquals("test", ListUtils.firstOrNull(List.of("test")));
    }

    @Test
    public void firstOrNull__should_return_null_if_empty() {
        assertNull(ListUtils.firstOrNull(Collections.emptyList()));
    }

    @Test
    public void firstOrNull__should_return_null_if_list_is_null() {
        assertNull(ListUtils.firstOrNull(null));
    }

}
