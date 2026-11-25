package com.example.rikua;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminPendaftaranItemTest {

    @Test
    public void searchesTicketAndBothNamesWithoutCaseSensitivity() {
        AdminPendaftaranItem item = item("RKA-2026-00007", "dikirim",
                "Ahmad Fauzi", "Siti Aminah", "2026-11-23 09:00:00");

        assertTrue(item.matchesFilters("rka-2026", null, null, null));
        assertTrue(item.matchesFilters("FAUZI", null, null, null));
        assertTrue(item.matchesFilters("aminah", null, null, null));
        assertFalse(item.matchesFilters("tidak ada", null, null, null));
    }

    @Test
    public void appliesActualStatusAndInclusiveSubmissionDateRange() {
        AdminPendaftaranItem item = item("RKA-2026-00007", "dikirim",
                "Ahmad", "Siti", "2026-11-23 09:00:00");

        assertTrue(item.matchesFilters("", "dikirim",
                "2026-11-23", "2026-11-23"));
        assertFalse(item.matchesFilters("", "draf", null, null));
        assertFalse(item.matchesFilters("", null,
                "2026-11-24", null));
    }

    private AdminPendaftaranItem item(
            String ticket,
            String status,
            String husband,
            String wife,
            String submissionDate
    ) {
        return new AdminPendaftaranItem(
                7,
                ticket,
                status,
                submissionDate,
                "2026-12-01 08:30:00",
                "KUA Randudongkal",
                husband,
                wife
        );
    }
}
