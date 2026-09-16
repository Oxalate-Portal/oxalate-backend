package io.oxalate.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

class StatsServiceUTC {

    @Test
    void yearlyRegistrationsReturnsValuesAndCumulativeValues() {
        var entityManager = Mockito.mock(EntityManager.class);
        var query = Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(Mockito.anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(
                new Object[] { BigDecimal.valueOf(2024), 2L },
                new Object[] { BigDecimal.valueOf(2025), 3L }));

        var result = new StatsService(entityManager, null).getYearlyRegistrations();

        assertEquals(4, result.size());
        assertEquals("registrations", result.get(0)
                                            .getType());
        assertEquals(5L, result.get(3)
                               .getValue());
    }

    @Test
    void yearlyPaymentsReturnsEmptyResultWithoutRows() {
        var entityManager = Mockito.mock(EntityManager.class);
        var query = Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(Mockito.anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertEquals(List.of(), new StatsService(entityManager, null).getYearlyPayments());
    }

    @Test
    void eventTypesUseResultColumnAsType() {
        var entityManager = Mockito.mock(EntityManager.class);
        var query = Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(Mockito.anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.<Object[]>of(new Object[] {
                BigDecimal.valueOf(2025), 4L, "CAVE" }));

        var result = new StatsService(entityManager, null).getEventTypesPerYear();

        assertEquals(1, result.size());
        assertEquals("CAVE", result.getFirst()
                                   .getType());
        assertEquals(4L, result.getFirst()
                               .getValue());
    }

    @Test
    void yearlyOrganizersAccumulatePerOrganizer() {
        var entityManager = Mockito.mock(EntityManager.class);
        var query = Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(Mockito.anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(
                new Object[] { "Alice Diver", BigDecimal.valueOf(2024), 2L },
                new Object[] { "Alice Diver", BigDecimal.valueOf(2025), 3L }));

        var result = new StatsService(entityManager, null).getYearlyOrganizers();

        assertEquals(4, result.size());
        assertEquals(5L, result.get(3)
                               .getValue());
        assertEquals("cumulative-Alice Diver", result.get(3)
                                                     .getType());
    }

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
