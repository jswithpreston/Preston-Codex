package com.preston.codex.domain.tasks.service;

import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.RecurrenceType;

import java.time.LocalDate;
import java.util.Objects;

public record RecurrenceRuleParams(
        RecurrenceType type,
        int interval,
        String weekdays,
        LocalDate startDate,
        LocalDate endDate,
        Integer occurrenceCount,
        Integer anchorDayOfMonth,
        MissedPolicy missedPolicy
) {
    public RecurrenceRuleParams {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
    }
}
