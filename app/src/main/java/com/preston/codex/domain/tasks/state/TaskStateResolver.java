package com.preston.codex.domain.tasks.state;

import com.preston.codex.domain.tasks.model.DerivedTaskState;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskStatus;

import java.time.LocalDate;
import java.util.Objects;

public final class TaskStateResolver {

    private TaskStateResolver() {
    }

    public static TaskStateResult resolve(TaskStateInput input) {
        Objects.requireNonNull(input, "input must not be null");

        // Terminal states
        if (input.status() == TaskStatus.COMPLETED) {
            return new TaskStateResult(DerivedTaskState.COMPLETED, false);
        }
        if (input.status() == TaskStatus.CANCELLED) {
            return new TaskStateResult(DerivedTaskState.CANCELLED, false);
        }

        // Unscheduled (NO_DATE) for one-off tasks with no date
        if (input.kind() == TaskKind.ONE_OFF && input.date() == null) {
            return new TaskStateResult(DerivedTaskState.NO_DATE, false);
        }

        LocalDate date = input.date();
        LocalDate today = input.today();
        boolean isOverdue = date != null && date.isBefore(today);

        if (input.status() == TaskStatus.IN_PROGRESS) {
            return new TaskStateResult(DerivedTaskState.IN_PROGRESS, isOverdue);
        }

        if (input.status() == TaskStatus.OPEN) {
            // Amendment 5: POSTPONED = not terminal AND date >= today AND hasRescheduleHistory
            if (date != null && !date.isBefore(today) && input.hasRescheduleHistory()) {
                return new TaskStateResult(DerivedTaskState.POSTPONED, false);
            }
            return new TaskStateResult(DerivedTaskState.SCHEDULED, isOverdue);
        }

        throw new IllegalStateException("Unexpected task status: " + input.status());
    }
}
