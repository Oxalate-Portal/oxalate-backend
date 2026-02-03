package io.oxalate.backend.service;

import io.oxalate.backend.api.PortalConfigEnum;
import io.oxalate.backend.api.response.stats.AggregateResponse;
import io.oxalate.backend.api.response.stats.DiverListItemResponse;
import io.oxalate.backend.api.response.stats.EventPeriodReportResponse;
import io.oxalate.backend.api.response.stats.EventReportResponse;
import io.oxalate.backend.api.response.stats.MultiYearValueResponse;
import io.oxalate.backend.api.response.stats.YearlyDiversListResponse;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class StatsService {
    private final EntityManager entityManager;
    private final PortalConfigurationService portalConfigurationService;

    public List<MultiYearValueResponse> getYearlyRegistrations() {

        var queryString = """
                SELECT
                  EXTRACT(YEAR FROM u.registered) AS user_year,
                  COUNT(u.id) AS registration_count
                FROM users u
                GROUP BY user_year
                ORDER BY user_year
                """;
        return getMultiYearValues(queryString, "registrations", true);
    }

    public List<MultiYearValueResponse> getYearlyEvents() {
        var queryString = """
                SELECT
                  EXTRACT(YEAR FROM e.start_time) AS year,
                  COUNT(e.id) AS event_count
                FROM events e
                WHERE e.start_time < NOW()
                  AND e.status = 'HELD'
                GROUP BY year
                ORDER BY year
                """;

        return getMultiYearValues(queryString, "events", true);
    }

    @SuppressWarnings("unchecked")
    public List<MultiYearValueResponse> getYearlyOrganizers() {
        var queryString = """
                SELECT
                  CONCAT(u.first_name, ' ', u.last_name) AS organizer_name,
                  EXTRACT(YEAR FROM e.start_time) AS year,
                  COUNT(e.id) AS event_count
                FROM events e,
                     users u
                WHERE u.id = e.organizer_id
                  AND e.status = 'HELD'
                  AND e.start_time < NOW()
                GROUP BY year, e.organizer_id, organizer_name
                ORDER BY year, event_count DESC
                """;

        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> results = query.getResultList();

        var multiYearValues = new ArrayList<MultiYearValueResponse>();

        if (!results.isEmpty()) {
            var cumulativeHash = new HashMap<String, Long>();

            for (Object[] o : results) {
                var organizerName = (String) o[0];
                var year = (BigDecimal) o[1];
                var eventCount = (Long) o[2];

                if (cumulativeHash.containsKey(organizerName)) {
                    cumulativeHash.put(organizerName, cumulativeHash.get(organizerName) + eventCount);
                } else {
                    cumulativeHash.put(organizerName, eventCount);
                }

                var response = MultiYearValueResponse.builder()
                                             .year(year.longValue())
                                             .value(eventCount)
                                             .type(organizerName)
                                             .build();
                multiYearValues.add(response);
                var responseCum = MultiYearValueResponse.builder()
                                                .year(year.longValue())
                                                .value(cumulativeHash.get(organizerName))
                                                .type("cumulative-" + organizerName)
                                                .build();
                multiYearValues.add(responseCum);
            }
        }

        return multiYearValues;
    }

    @SuppressWarnings("unchecked")
    public List<MultiYearValueResponse> getYearlyPayments() {
        var queryString = """
                SELECT
                  EXTRACT(YEAR FROM p.created) AS year,
                  p.payment_type,
                  COUNT(p.id) AS payment_count
                FROM payments p
                GROUP BY year, p.payment_type
                ORDER BY year, payment_count DESC
                """;

        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> results = query.getResultList();

        var multiYearValues = new ArrayList<MultiYearValueResponse>();

        if (!results.isEmpty()) {
            for (Object[] o : results) {
                var year = (BigDecimal) o[0];
                var paymentType = (String) o[1];
                var paymentCount = (Long) o[2];

                var response = MultiYearValueResponse.builder()
                                             .year(year.longValue())
                                             .value(paymentCount)
                                             .type(paymentType)
                                             .build();
                multiYearValues.add(response);
            }
        }

        return multiYearValues;
    }

    @SuppressWarnings("unchecked")
    public List<EventPeriodReportResponse> getEventReports() {
        var eventPeriodReportResponses = new ArrayList<EventPeriodReportResponse>();

        // First get the date of the first event
        var queryString = """
                SELECT
                    EXTRACT(YEAR FROM MIN(e.start_time)) AS year,
                    EXTRACT(MONTH FROM MIN(e.start_time)) AS month
                FROM events e
                """;
        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> dateResult = query.getResultList();

        if (!dateResult.isEmpty() && dateResult.get(0) != null && dateResult.get(0)[0] != null) {
            BigDecimal startYear = (BigDecimal) dateResult.get(0)[0];
            BigDecimal startMonth = (BigDecimal) dateResult.get(0)[1];

            var yearHalf = 1;
            var currentYear = Year.now()
                                  .getValue();

            if (startMonth.intValue() > 6) {
                yearHalf = 7;
            }

            for (int fetchYear = startYear.intValue(); fetchYear <= currentYear; fetchYear++) {
                if (yearHalf == 1) {
                    // get list of all events for the first half year of the given year
                    addPeriodEvents(fetchYear, yearHalf, eventPeriodReportResponses);
                    yearHalf = 7;
                }

                // get list of all events for the second half year of the given year
                addPeriodEvents(fetchYear, yearHalf, eventPeriodReportResponses);

                yearHalf = 1;
            }
        }

        return eventPeriodReportResponses;
    }

    @SuppressWarnings("unchecked")
    public List<YearlyDiversListResponse> getYearlyDiversList() {
        var yearlyList = new ArrayList<YearlyDiversListResponse>();
        var firstYear = getOldestEventYear();
        var diverListSize = portalConfigurationService.getNumericConfiguration(PortalConfigEnum.GENERAL.group,
                PortalConfigEnum.GeneralConfigEnum.TOP_DIVER_LIST_SIZE.key);
        // If we do not have any data, then we return empty list
        if (firstYear == 0L) {
            log.warn("No dives found for top 50 divers");
            return yearlyList;
        }

        var lastYear = Year.now()
                           .getValue();

        for (var i = firstYear; i <= lastYear; i++) {
            var queryString = String.format("""
                    SELECT u.id,
                           CONCAT(u.first_name, ' ', u.last_name),
                           SUM(ep.dive_count) AS diveCount
                    FROM users u,
                         event_participants ep,
                         events e
                    WHERE u.id = ep.user_id
                      AND e.id = ep.event_id
                      AND e.start_time < NOW()
                      AND EXTRACT('Year' FROM e.start_time) = %d
                    GROUP BY u.id
                    ORDER BY diveCount DESC
                    LIMIT %d
                    """, i, diverListSize);

            var query = entityManager.createNativeQuery(queryString);
            List<Object[]> results = query.getResultList();

            var position = 1;
            var entryList = new ArrayList<DiverListItemResponse>();

            for (Object[] o : results) {
                var userId = (Long) o[0];
                var userName = (String) o[1];
                var diveCount = (Long) o[2];
                var diverListItemResponse = DiverListItemResponse.builder()
                                                                 .userId(userId)
                                                                 .userName(userName)
                                                                 .diveCount(diveCount)
                                                                 .position(position)
                                                                 .build();
                entryList.add(diverListItemResponse);
                position++;
            }

            var yearlyDiversListResponse = YearlyDiversListResponse.builder()
                                                                   .year(i)
                                                                   .divers(entryList)
                                                                   .build();
            yearlyList.add(yearlyDiversListResponse);
        }

        return yearlyList;
    }

    public AggregateResponse getAggregateData() {
        var diversPerYear = getDiversPerYear();
        var diverTypePerYear = getDiverTypesPerYear();
        var eventsPerYear = getEventsPerYear();
        var eventTypesPerYear = getEventTypesPerYear();
        return AggregateResponse.builder()
                                .diversPerYear(diversPerYear)
                                .diverTypesPerYear(diverTypePerYear)
                                .eventsPerYear(eventsPerYear)
                                .eventTypesPerYear(eventTypesPerYear)
                                .build();
    }

    public List<MultiYearValueResponse> getEventsPerYear() {
        var queryString = """
                SELECT
                  EXTRACT(YEAR FROM e.start_time) AS year,
                  COUNT(e.id) AS event_count
                FROM events e
                WHERE e.status = 'HELD'
                GROUP BY year
                ORDER BY year;
                """;
        return getMultiYearValues(queryString, "events", false);
    }

    public List<MultiYearValueResponse> getEventTypesPerYear() {
        var queryString = """
                SELECT
                EXTRACT(YEAR FROM e.start_time) AS event_year,
                COUNT(e.id) AS event_count,
                e.TYPE AS event_type
                FROM events e
                WHERE e.status = 'HELD'
                GROUP BY event_year, event_type
                ORDER BY event_year;
                """;
        return getMultiYearValues(queryString, "2", false);
    }

    public List<MultiYearValueResponse> getDiversPerYear() {
        var queryString = """
                SELECT
                  EXTRACT(YEAR FROM e.start_time) AS event_year,
                  COUNT(e.id) AS registration_count
                FROM events e, event_participants ep
                WHERE e.id = ep.event_id
                  AND e.status = 'HELD'
                GROUP BY event_year
                ORDER BY event_year
                """;
        return getMultiYearValues(queryString, "divers", false);
    }

    public List<MultiYearValueResponse> getDiverTypesPerYear() {
        var queryString = """
                SELECT
                  EXTRACT(YEAR FROM e.start_time) AS event_year,
                  COUNT(e.id) AS registration_count,
                  ep.event_user_type
                FROM events e, event_participants ep
                WHERE e.id = ep.event_id
                  AND e.status = 'HELD'
                GROUP BY event_year, ep.event_user_type
                ORDER BY event_year
                """;
        return getMultiYearValues(queryString, "2", false);
    }

    private long getOldestEventYear() {
        var queryString = "SELECT MIN(EXTRACT('Year' FROM e.start_time))"
                + "FROM events e";

        var query = entityManager.createNativeQuery(queryString);

        if (query != null) {
            var possibleValue = query.getSingleResult();

            if (possibleValue != null) {
                var result = (BigDecimal) possibleValue;
                return result.longValue();
            }
        }

        return 0L;
    }

    private void addPeriodEvents(int fetchYear, int yearHalf, ArrayList<EventPeriodReportResponse> eventPeriodReportResponses) {
        var reports = getAllEventsForPeriod(fetchYear, yearHalf);
        var eventPeriodReport = EventPeriodReportResponse.builder()
                                                         .periodStart(Instant.parse(fetchYear + "-0" + yearHalf + "-01T00:00:00.00Z"))
                                                         .period(fetchYear + "-" + yearHalf)
                                                         .events(reports)
                                                         .build();
        eventPeriodReportResponses.add(eventPeriodReport);
    }

    @SuppressWarnings("unchecked")
    private List<EventReportResponse> getAllEventsForPeriod(long fetchYear, int yearHalf) {
        var queryString = String.format("""
                SELECT e.id,
                       e.start_time,
                       CONCAT(u.first_name, ' ', u.last_name) AS organizer_name,
                       COUNT(ep.user_id) AS participant_count,
                       SUM(ep.dive_count) AS dive_sum
                FROM events e,
                     users u,
                     event_participants ep
                WHERE e.organizer_id = u.id
                  AND e.status = 'HELD'
                  AND ep.event_id = e.id
                  AND e.start_time < NOW()
                  AND e.start_time > DATE '%s-0%s-01'
                  AND e.start_time < DATE '%s-0%s-01' + INTERVAL '6 months'
                GROUP BY e.id, organizer_name, e.start_time
                ORDER BY e.start_time
                """, fetchYear, yearHalf, fetchYear, yearHalf);

        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> results;
        var eventReportResponses = new ArrayList<EventReportResponse>();

        try {
            results = query.getResultList();
        } catch (Exception e) {
            log.error("Error in query: {}{}", queryString, e.getMessage(), e);
            return eventReportResponses;
        }

        for (Object[] o : results) {
            var eventId = (Long) o[0];
            var eventDateTime = ((java.sql.Timestamp) o[1]).toInstant();
            var organizerName = (String) o[2];
            var eventCount = (Long) o[3];
            var diveCount = (Long) o[4];

            var response = EventReportResponse.builder()
                                              .eventId(eventId)
                                              .eventDateTime(eventDateTime)
                                              .organizerName(organizerName)
                                              .participantCount(eventCount.intValue())
                                              .diveCount(diveCount.intValue())
                                              .build();
            eventReportResponses.add(response);
        }

        return eventReportResponses;
    }

    /**
     * Generic method to get multi-year values from a query, if the keyName is numeric, it is treated as the index of the column to use as type. Note that this
     * index must be greater than 1, as 0 is year and 1 is value.
     *
     * @param queryString       SQL query string
     * @param keyName           the name of the key or the index of the column to use as type
     * @param includeCumulative whether to include cumulative values
     * @return list of MultiYearValueResponse
     */
    @SuppressWarnings("unchecked")
    private List<MultiYearValueResponse> getMultiYearValues(String queryString, String keyName, boolean includeCumulative) {
        var isKeyNumeric = keyName.matches("\\d+");
        var keyIndex = isKeyNumeric ? Integer.parseInt(keyName) : -1;

        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> results = query.getResultList();

        var multiYearValues = new ArrayList<MultiYearValueResponse>();

        if (!results.isEmpty()) {
            var cumulative = 0L;

            for (Object[] o : results) {
                var year = (BigDecimal) o[0];
                var count = (Long) o[1];

                if (isKeyNumeric) {
                    keyName = (String) o[keyIndex];
                }

                cumulative += count;

                var response = MultiYearValueResponse.builder()
                                             .year(year.longValue())
                                             .value(count)
                                             .type(keyName)
                                             .build();
                multiYearValues.add(response);

                if (includeCumulative) {
                    var responseCum = MultiYearValueResponse.builder()
                                                            .year(year.longValue())
                                                            .value(cumulative)
                                                            .type("cumulative")
                                                            .build();
                    multiYearValues.add(responseCum);
                }
            }
        }

        return multiYearValues;
    }
}
