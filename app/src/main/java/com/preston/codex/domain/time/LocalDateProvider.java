package com.preston.codex.domain.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Pure domain provider for local dates and times based on an injected Clock and ZoneId supplier.
 * Safe across time zone changes, DST transitions, and midnight-skipping offsets.
 */
public final class LocalDateProvider {

    private final Clock clock;
    private final Supplier<ZoneId> zoneSupplier;

    public LocalDateProvider(Clock clock, Supplier<ZoneId> zoneSupplier) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zoneSupplier = Objects.requireNonNull(zoneSupplier, "zoneSupplier must not be null");
    }

    public LocalDate getToday() {
        return LocalDate.now(clock.withZone(getZoneId()));
    }

    public LocalTime getLocalTime() {
        return LocalTime.now(clock.withZone(getZoneId()));
    }

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.now(clock.withZone(getZoneId()));
    }

    public Instant getInstant() {
        return clock.instant();
    }

    public ZoneId getZoneId() {
        ZoneId zoneId = zoneSupplier.get();
        return Objects.requireNonNull(zoneId, "zoneSupplier returned null ZoneId");
    }

    public Clock getClock() {
        return clock;
    }

    public Supplier<ZoneId> getZoneSupplier() {
        return zoneSupplier;
    }
}
