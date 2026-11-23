package com.example.rikua;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SessionExpiryTest {

    @Test
    public void parsesEpochSecondsAndMilliseconds() {
        assertEquals(Long.valueOf(1_800_000_000_000L),
                SessionExpiry.parseMillis("1800000000"));
        assertEquals(Long.valueOf(1_800_000_000_000L),
                SessionExpiry.parseMillis("1800000000000"));
    }

    @Test
    public void parsesIsoUtcAndOffsetTimestampsAsSameInstant() {
        Long utc = SessionExpiry.parseMillis("2026-11-23T04:00:00Z");
        Long offset = SessionExpiry.parseMillis("2026-11-23T11:00:00+07:00");

        assertEquals(utc, offset);
    }

    @Test
    public void parsesIsoFractionalAndSqlTimesInJakartaTimezone() {
        Long iso = SessionExpiry.parseMillis("2026-11-23T11:00:00.123+07:00");
        Long sql = SessionExpiry.parseMillis("2026-11-23 11:00:00.123");

        assertEquals(iso, sql);
    }

    @Test
    public void rejectsMissingMalformedAndExpiredValues() {
        assertNull(SessionExpiry.parseMillis(null));
        assertNull(SessionExpiry.parseMillis(""));
        assertNull(SessionExpiry.parseMillis("not-a-date"));
        assertNull(SessionExpiry.parseMillis("2026-02-30T12:00:00Z"));
        assertFalse(SessionExpiry.isFuture("1800000000", 1_800_000_000_000L));
        assertTrue(SessionExpiry.isFuture("1800000001", 1_800_000_000_000L));
    }
}
