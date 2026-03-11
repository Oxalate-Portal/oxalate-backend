package io.oxalate.backend.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class StatsServiceUTC {

    @Test
    void mapToInstantLocalDateTimeOk() throws Exception {
        var service = new StatsService(null, null);
        var value = LocalDateTime.of(2026, 3, 11, 16, 53, 1);

        var result = invokeMapToInstant(service, value);

        assertEquals(value.atZone(ZoneId.systemDefault())
                          .toInstant(), result);
    }

    @Test
    void mapToInstantTimestampOk() throws Exception {
        var service = new StatsService(null, null);
        var value = Timestamp.from(Instant.parse("2026-03-11T14:53:01Z"));

        var result = invokeMapToInstant(service, value);

        assertEquals(value.toInstant(), result);
    }

    @Test
    void mapToInstantUnsupportedTypeOk() throws Exception {
        var service = new StatsService(null, null);

        var result = invokeMapToInstant(service, 123);

        assertNull(result);
    }

    private Instant invokeMapToInstant(StatsService service, Object value) throws Exception {
        var method = StatsService.class.getDeclaredMethod("mapToInstant", Object.class);
        method.setAccessible(true);
        return (Instant) method.invoke(service, value);
    }
}

