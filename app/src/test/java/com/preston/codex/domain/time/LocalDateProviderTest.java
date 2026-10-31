package com.preston.codex.domain.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public class LocalDateProviderTest {

    @Test
    public void dstDay_springForward_correctlyCalculatesLocalDate() {
        // America/New_York on 2026-03-08 spring forward (02:00 -> 03:00)
        // 06:30:00 UTC corresponds to 01:30:00 EST (UTC-5)
        Instant beforeTransition = Instant.parse("2026-03-08T06:30:00Z");
        Clock clockBefore = Clock.fixed(beforeTransition, ZoneOffset.UTC);
        LocalDateProvider providerBefore = new LocalDateProvider(clockBefore, () -> ZoneId.of("America/New_York"));
        assertEquals(LocalDate.of(2026, 3, 8), providerBefore.getToday());

        // 07:30:00 UTC corresponds to 03:30:00 EDT (UTC-4)
        Instant afterTransition = Instant.parse("2026-03-08T07:30:00Z");
        Clock clockAfter = Clock.fixed(afterTransition, ZoneOffset.UTC);
        LocalDateProvider providerAfter = new LocalDateProvider(clockAfter, () -> ZoneId.of("America/New_York"));
        assertEquals(LocalDate.of(2026, 3, 8), providerAfter.getToday());
    }

    @Test
    public void dstDay_fallBack_correctlyCalculatesLocalDate() {
        // America/New_York on 2026-11-01 fall back (02:00 EDT -> 01:00 EST)
        // 05:30:00 UTC corresponds to 01:30:00 EDT
        Instant instant1 = Instant.parse("2026-11-01T05:30:00Z");
        Clock clock1 = Clock.fixed(instant1, ZoneOffset.UTC);
        LocalDateProvider provider1 = new LocalDateProvider(clock1, () -> ZoneId.of("America/New_York"));
        assertEquals(LocalDate.of(2026, 11, 1), provider1.getToday());

        // 06:30:00 UTC corresponds to 01:30:00 EST
        Instant instant2 = Instant.parse("2026-11-01T06:30:00Z");
        Clock clock2 = Clock.fixed(instant2, ZoneOffset.UTC);
        LocalDateProvider provider2 = new LocalDateProvider(clock2, () -> ZoneId.of("America/New_York"));
        assertEquals(LocalDate.of(2026, 11, 1), provider2.getToday());
    }

    @Test
    public void zoneWhereDstSkipsMidnight_americaHavana_correctlyCalculatesLocalDate() {
        // In America/Havana, DST transitions have historically shifted at 00:00 (midnight).
        // For instance, on 2000-10-15 at 00:00:00, clocks moved to 01:00:00 (skipping midnight hour).
        // 03:59:59 UTC on 2000-10-15 corresponds to 23:59:59 on 2000-10-14 in Havana (UTC-4)
        Instant justBeforeMidnight = Instant.parse("2000-10-15T03:59:59Z");
        Clock clockBefore = Clock.fixed(justBeforeMidnight, ZoneOffset.UTC);
        LocalDateProvider providerBefore = new LocalDateProvider(clockBefore, () -> ZoneId.of("America/Havana"));
        assertEquals(LocalDate.of(2000, 10, 14), providerBefore.getToday());

        // 04:00:00 UTC on 2000-10-15 corresponds to 01:00:00 on 2000-10-15 in Havana
        Instant justAfterMidnight = Instant.parse("2000-10-15T04:00:00Z");
        Clock clockAfter = Clock.fixed(justAfterMidnight, ZoneOffset.UTC);
        LocalDateProvider providerAfter = new LocalDateProvider(clockAfter, () -> ZoneId.of("America/Havana"));
        assertEquals(LocalDate.of(2000, 10, 15), providerAfter.getToday());
    }

    @Test
    public void zoneChange_dynamicallyReflectsNewZoneForSameInstant() {
        Instant pinned = Instant.parse("2026-10-31T20:00:00Z");
        Clock clock = Clock.fixed(pinned, ZoneOffset.UTC);
        AtomicReference<ZoneId> currentZone = new AtomicReference<>(ZoneId.of("Asia/Tokyo"));

        LocalDateProvider provider = new LocalDateProvider(clock, currentZone::get);

        // In Tokyo (+09:00), 20:00 UTC on Oct 31 is 05:00 on Nov 1
        assertEquals(LocalDate.of(2026, 11, 1), provider.getToday());

        // Change zone to Los Angeles (-07:00 / PDT)
        currentZone.set(ZoneId.of("America/Los_Angeles"));
        // In Los Angeles, 20:00 UTC on Oct 31 is 13:00 on Oct 31
        assertEquals(LocalDate.of(2026, 10, 31), provider.getToday());
    }

    @Test
    public void getters_returnNonNullComponents() {
        Clock clock = Clock.systemUTC();
        LocalDateProvider provider = new LocalDateProvider(clock, () -> ZoneOffset.UTC);
        assertNotNull(provider.getClock());
        assertNotNull(provider.getZoneSupplier());
        assertNotNull(provider.getZoneId());
        assertNotNull(provider.getToday());
        assertNotNull(provider.getLocalTime());
        assertNotNull(provider.getLocalDateTime());
        assertNotNull(provider.getInstant());
    }
}
