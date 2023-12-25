package io.oxalate.backend.service;

import io.oxalate.backend.api.response.stats.DiverListItemResponse;
import io.oxalate.backend.api.response.stats.EventPeriodReportResponse;
import io.oxalate.backend.api.response.stats.EventReportResponse;
import io.oxalate.backend.api.response.stats.MultiYearValue;
import io.oxalate.backend.api.response.stats.YearlyDiversListResponse;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
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

    public List<MultiYearValue> getYearlyRegistrations() {

        var queryString = "SELECT EXTRACT(YEAR FROM u.registered) AS year, COUNT(u.id) AS registration_count FROM users u GROUP BY year ORDER BY year";
        return getMultiYearValues(queryString, "registrations");
    }

    public List<MultiYearValue> getYearlyEvents() {
        var queryString = "SELECT EXTRACT(YEAR FROM e.start_time) AS year, COUNT(e.id) AS event_count FROM events e WHERE e.start_time < NOW() GROUP BY year ORDER BY year";
        return getMultiYearValues(queryString, "events");
    }

    public List<MultiYearValue> getYearlyOrganizers() {
        var queryString = "SELECT "
                + "  CONCAT(u.first_name, ' ', u.last_name) AS organizer_name, "
                + "  EXTRACT(YEAR FROM e.start_time) AS year, "
                + "  COUNT(e.id) AS event_count "
                + "FROM events e, "
                + "users u "
                + "WHERE u.id = e.organizer_id "
                + "  AND e.start_time < NOW() "
                + "GROUP BY YEAR, e.organizer_id, organizer_name "
                + "ORDER BY YEAR, event_count DESC";

        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> results = query.getResultList();

        var multiYearValues = new ArrayList<MultiYearValue>();

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

                var response = MultiYearValue.builder()
                                             .year(year.longValue())
                                             .value(eventCount)
                                             .type(organizerName)
                                             .build();
                multiYearValues.add(response);
                var responseCum = MultiYearValue.builder()
                                                .year(year.longValue())
                                                .value(cumulativeHash.get(organizerName))
                                                .type("cumulative-" + organizerName)
                                                .build();
                multiYearValues.add(responseCum);
            }
        }

        return multiYearValues;
    }

    public List<MultiYearValue> getYearlyPayments() {
        var queryString = "SELECT "
                + "  EXTRACT(YEAR FROM p.created_at) AS year, "
                + "  p.payment_type,"
                + "  COUNT(p.id) AS payment_count "
                + "FROM payments p "
                + "GROUP BY year, p.payment_type "
                + "ORDER BY year, payment_count DESC";

        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> results = query.getResultList();

        var multiYearValues = new ArrayList<MultiYearValue>();

        if (!results.isEmpty()) {
            for (Object[] o : results) {
                var year = (BigDecimal) o[0];
                var paymentType = (String) o[1];
                var paymentCount = (Long) o[2];

                var response = MultiYearValue.builder()
                                             .year(year.longValue())
                                             .value(paymentCount)
                                             .type(paymentType)
                                             .build();
                multiYearValues.add(response);
            }
        }

        return multiYearValues;
    }

    public List<EventPeriodReportResponse> getEventReports() {
        var eventPeriodReportResponses = new ArrayList<EventPeriodReportResponse>();

        // First get the date of the first event
        var queryString = "SELECT EXTRACT(YEAR FROM MIN(e.start_time)) AS year, EXTRACT(MONTH FROM MIN(e.start_time)) AS month FROM events e";
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

    public List<YearlyDiversListResponse> getYearlyDiversList() {
        var yearlyList = new ArrayList<YearlyDiversListResponse>();
        var firstYear = getOldestEventYear();

        // If we do not have any data, then we return empty list
        if (firstYear == 0L) {
            log.warn("No dives found for top 50 divers");
            return yearlyList;
        }

        var lastYear = Year.now()
                           .getValue();

        for (var i = firstYear; i <= lastYear; i++) {
            var queryString = "SELECT u.id, CONCAT(u.first_name, ' ', u.last_name)"
                    + "  , sum(ep.dive_count) AS diveCount "
                    + " , privacy "
                    + "FROM users u , event_participants ep, events e "
                    + "WHERE u.id = ep.user_id "
                    + "  AND e.id = ep.event_id "
                    + "  AND e.start_time <NOW() "
                    + "  AND EXTRACT('Year' FROM e.start_time) = " + i + " "
                    + "GROUP BY u.id "
                    + "ORDER BY diveCount DESC "
                    + "LIMIT 50";

            var query = entityManager.createNativeQuery(queryString);
            List<Object[]> results = query.getResultList();

            var position = 1;
            var entryList = new ArrayList<DiverListItemResponse>();

            for (Object[] o : results) {
                var userId = (Long) o[0];
                var userName = (String) o[1];
                var diveCount = (Long) o[2];
                var privacy = (Boolean) o[3];
                var diverListItemResponse = DiverListItemResponse.builder()
                                                                 .userId(userId)
                                                                 .userName((privacy ? "Piilotettu" : userName))
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

    private List<EventReportResponse> getAllEventsForPeriod(long fetchYear, int yearHalf) {
        var queryString = "SELECT e.id, "
                + "    e.start_time, "
                + "    CONCAT(u.first_name, ' ', u.last_name) AS organizer_name, "
                + "    COUNT(ep.user_id) AS participant_count, "
                + "    SUM(ep.dive_count) AS dive_sum "
                + "FROM events e  "
                + "  , users u "
                + "  , event_participants ep  "
                + "WHERE e.organizer_id = u.id  "
                + "  AND ep.event_id = e.id "
                + "  AND e.start_time < NOW() "
                + "  AND e.start_time > date '" + fetchYear + "-0" + yearHalf + "-01' "
                + "  AND e.start_time < date '" + fetchYear + "-0" + yearHalf + "-01' + INTERVAL '6 months' "
                + "GROUP BY e.id, organizer_name, e.start_time "
                + "ORDER BY e.start_time";

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
            var eventDateTime = (Timestamp) o[1];
            var organizerName = (String) o[2];
            var eventCount = (Long) o[3];
            var diveCount = (Long) o[4];

            var response = EventReportResponse.builder()
                                              .eventId(eventId)
                                              .eventDateTime(eventDateTime.toInstant())
                                              .organizerName(organizerName)
                                              .participantCount(eventCount.intValue())
                                              .diveCount(diveCount.intValue())
                                              .build();
            eventReportResponses.add(response);
        }

        return eventReportResponses;
    }

    private List<MultiYearValue> getMultiYearValues(String queryString, String keyName) {
        var query = entityManager.createNativeQuery(queryString);
        List<Object[]> results = query.getResultList();

        var multiYearValues = new ArrayList<MultiYearValue>();

        if (!results.isEmpty()) {
            var cumulative = 0L;

            for (Object[] o : results) {
                var year = (BigDecimal) o[0];
                var count = (Long) o[1];
                cumulative += count;

                var response = MultiYearValue.builder()
                                             .year(year.longValue())
                                             .value(count)
                                             .type(keyName)
                                             .build();
                multiYearValues.add(response);
                var responseCum = MultiYearValue.builder()
                                                .year(year.longValue())
                                                .value(cumulative)
                                                .type("cumulative")
                                                .build();
                multiYearValues.add(responseCum);
            }
        }

        return multiYearValues;
    }
}
