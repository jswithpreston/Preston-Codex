package com.preston.codex.domain.tasks.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.tasks.model.DerivedTaskState;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskStatus;

import org.junit.Test;

import java.time.LocalDate;

public class TaskStateResolverTest {

    private final LocalDate today = LocalDate.of(2026, 10, 30);
    private final LocalDate past = LocalDate.of(2026, 10, 20);
    private final LocalDate future = LocalDate.of(2026, 11, 5);

    @Test
    public void completed_pastDate_returnsCompletedNotOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.COMPLETED, past, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.COMPLETED, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void completed_futureDate_returnsCompletedNotOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.COMPLETED, future, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.COMPLETED, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void cancelled_pastDate_returnsCancelledNotOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.CANCELLED, past, today, true, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.CANCELLED, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void oneOff_nullDate_returnsNoDate() {
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, null, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.NO_DATE, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void inProgress_futureDate_returnsInProgressNotOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.IN_PROGRESS, future, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.IN_PROGRESS, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void inProgress_pastDate_returnsInProgressIsOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.IN_PROGRESS, past, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.IN_PROGRESS, result.state());
        assertTrue(result.isOverdue());
    }

    @Test
    public void inProgress_today_returnsInProgressNotOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.IN_PROGRESS, today, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.IN_PROGRESS, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void open_today_noHistory_returnsScheduledNotOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, today, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.SCHEDULED, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void open_futureDate_noHistory_returnsScheduledNotOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, future, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.SCHEDULED, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void open_pastDate_noHistory_returnsScheduledIsOverdue() {
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, past, today, false, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.SCHEDULED, result.state());
        assertTrue(result.isOverdue());
    }

    @Test
    public void open_today_hasRescheduleHistory_returnsPostponed() {
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, today, today, true, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.POSTPONED, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void open_future_hasRescheduleHistory_returnsPostponed() {
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, future, today, true, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.POSTPONED, result.state());
        assertFalse(result.isOverdue());
    }

    @Test
    public void open_past_hasRescheduleHistory_returnsScheduledIsOverdue() {
        // Postponed requires date >= today. If a postponed task falls into the past, it becomes overdue.
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, past, today, true, TaskKind.ONE_OFF);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.SCHEDULED, result.state());
        assertTrue(result.isOverdue());
    }

    @Test
    public void seriesOccurrence_resolvesCorrectly() {
        TaskStateInput input = new TaskStateInput(TaskStatus.OPEN, today, today, false, TaskKind.SERIES);
        TaskStateResult result = TaskStateResolver.resolve(input);
        assertEquals(DerivedTaskState.SCHEDULED, result.state());
        assertFalse(result.isOverdue());
    }
}
