package com.preston.codex.domain.tasks.backlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RecurrenceType;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskPriority;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.testutil.DirectTransactionRunner;
import com.preston.codex.domain.tasks.testutil.InMemoryTaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class BacklogReviewServiceTest {

    private InMemoryTaskRepository repository;
    private LocalDateProvider dateProvider;
    private BacklogReviewService reviewService;
    private LocalDate today;
    private Instant now;

    @Before
    public void setUp() {
        repository = new InMemoryTaskRepository();
        today = LocalDate.of(2026, 5, 10);
        ZoneId zone = ZoneId.of("UTC");
        now = today.atStartOfDay(zone).toInstant();
        Clock clock = Clock.fixed(now, zone);
        dateProvider = new LocalDateProvider(clock, () -> zone);
        reviewService = new BacklogReviewService(repository, dateProvider, new DirectTransactionRunner());
    }

    private Task createOneOff(String title, LocalDate scheduledDate, LocalDate deadline, boolean isFlexible) {
        Task task = new Task(
                0L,
                TaskKind.ONE_OFF,
                title,
                "Description",
                TaskStatus.OPEN,
                null,
                null,
                null,
                isFlexible,
                TaskPriority.MEDIUM,
                deadline,
                30,
                scheduledDate,
                null,
                0,
                null,
                null,
                now,
                now,
                Collections.emptySet()
        );
        long id = repository.insertTask(task);
        return repository.findTaskById(id);
    }

    @Test
    public void preview_gathersOverdueOneOffsAndMissedOccurrences() {
        // Overdue one-off task
        Task overdueTask = createOneOff("Overdue OneOff", today.minusDays(2), today.plusDays(5), true);

        // Series task with missed occurrence
        Task series = new Task(0L, TaskKind.SERIES, "Daily Practice", null, TaskStatus.OPEN, null, null, null, true, TaskPriority.HIGH, null, 30, null, null, 0, null, null, now, now, Collections.emptySet());
        long seriesId = repository.insertTask(series);
        RecurrenceRule rule = new RecurrenceRule(0L, seriesId, RecurrenceType.DAILY, 1, null, today.minusDays(10), null, null, null, com.preston.codex.domain.tasks.model.MissedPolicy.KEEP_OVERDUE, now, now);
        long ruleId = repository.insertRecurrenceRule(rule);
        TaskOccurrence missedOcc = new TaskOccurrence(0L, seriesId, ruleId, today.minusDays(1), today.minusDays(1), OccurrenceStatus.OPEN, null, null, null, null, 0, null, null, now, now);
        repository.insertOccurrence(missedOcc);

        BacklogReviewPreview preview = reviewService.preview();
        assertEquals(2, preview.proposedMoves().size());
    }

    @Test
    public void apply_revalidatesAndAppliesMoves() {
        Task task = createOneOff("Review Draft", today.minusDays(1), today.plusDays(5), true);
        BacklogReviewPreview preview = reviewService.preview();
        assertEquals(1, preview.proposedMoves().size());

        BacklogReviewReport report = reviewService.apply(preview.proposedMoves());
        assertEquals(1, report.appliedMoves().size());
        assertTrue(report.skippedMoves().isEmpty());

        Task updated = repository.findTaskById(task.id());
        assertEquals(today, updated.scheduledDate());
        assertEquals(1, updated.autoRescheduleCount());

        List<RescheduleRecord> history = repository.findRescheduleRecordsByTaskId(task.id());
        assertEquals(1, history.size());
        assertEquals("BACKLOG_AUTO_RESCHEDULE", history.get(0).reason());

        List<TaskEvent> events = repository.findTaskEventsByTaskId(task.id());
        assertEquals(1, events.size());
        assertEquals("AUTO_RESCHEDULED", events.get(0).eventType());
    }

    @Test
    public void apply_skipsStaleMove_whenAlreadyCompleted() {
        Task task = createOneOff("Completed Mid-Flight", today.minusDays(1), today.plusDays(5), true);
        BacklogReviewPreview preview = reviewService.preview();
        assertEquals(1, preview.proposedMoves().size());

        // Complete the task before apply
        Task completed = new Task(task.id(), task.taskKind(), task.title(), task.description(), TaskStatus.COMPLETED, task.previousTaskId(), task.knowledgeEntryId(), task.reflection(), task.isFlexible(), task.priority(), task.deadline(), task.estimatedDurationMinutes(), task.scheduledDate(), task.scheduledTime(), task.autoRescheduleCount(), now, null, task.createdAt(), now, task.tags());
        repository.updateTask(completed);

        BacklogReviewReport report = reviewService.apply(preview.proposedMoves());
        assertTrue(report.appliedMoves().isEmpty());
        assertEquals(1, report.skippedMoves().size());
        assertEquals("ALREADY_COMPLETED", report.skippedMoves().get(0).reason());
    }

    @Test
    public void apply_skipsStaleMove_whenCapExceeded() {
        Task task = createOneOff("Cap Exceeded Task", today.minusDays(1), today.plusDays(5), true);
        BacklogReviewPreview preview = reviewService.preview();
        assertEquals(1, preview.proposedMoves().size());

        // Pre-fill target day (today) with 6 tasks before calling apply
        for (int i = 0; i < 6; i++) {
            createOneOff("Filler " + i, today, null, false);
        }

        BacklogReviewReport report = reviewService.apply(preview.proposedMoves());
        assertTrue(report.appliedMoves().isEmpty());
        assertEquals(1, report.skippedMoves().size());
        assertEquals("CAP_EXCEEDED", report.skippedMoves().get(0).reason());
    }

    @Test
    public void apply_skipsStaleMove_whenPastDeadline() {
        // Move proposed to today, but deadline updated/stale to yesterday
        Task task = createOneOff("Urgent Task", today.minusDays(2), today.minusDays(1), true);
        BacklogMoveProposal staleMove = new BacklogMoveProposal(task.id(), TaskKind.ONE_OFF, task.title(), today.minusDays(2), today, 30);

        BacklogReviewReport report = reviewService.apply(Collections.singletonList(staleMove));
        assertTrue(report.appliedMoves().isEmpty());
        assertEquals(1, report.skippedMoves().size());
        assertEquals("PAST_DEADLINE", report.skippedMoves().get(0).reason());
    }

    @Test
    public void apply_rollbackOnFailure_leavesDatabaseUnchanged() {
        // Amendment 5 requirement: failure mid-apply leaves DB unchanged
        FailingTransactionRunner failingRunner = new FailingTransactionRunner();
        reviewService = new BacklogReviewService(repository, dateProvider, failingRunner);

        Task task1 = createOneOff("Task 1", today.minusDays(2), today.plusDays(5), true);
        Task task2 = createOneOff("Task 2", today.minusDays(1), today.plusDays(5), true);

        List<BacklogMoveProposal> moves = new ArrayList<>();
        moves.add(new BacklogMoveProposal(task1.id(), TaskKind.ONE_OFF, task1.title(), today.minusDays(2), today, 30));
        moves.add(new BacklogMoveProposal(task2.id(), TaskKind.ONE_OFF, task2.title(), today.minusDays(1), today, 30));

        failingRunner.failDuringTransaction = true;

        try {
            reviewService.apply(moves);
            fail("Expected RuntimeException during transactional failure");
        } catch (RuntimeException expected) {
            // expected
        }

        // Database remains in original state
        Task check1 = repository.findTaskById(task1.id());
        Task check2 = repository.findTaskById(task2.id());
        assertEquals(today.minusDays(2), check1.scheduledDate());
        assertEquals(today.minusDays(1), check2.scheduledDate());
        assertEquals(0, check1.autoRescheduleCount());
        assertEquals(0, check2.autoRescheduleCount());
        assertTrue(repository.findRescheduleRecordsByTaskId(task1.id()).isEmpty());
        assertTrue(repository.findRescheduleRecordsByTaskId(task2.id()).isEmpty());
    }

    static class FailingTransactionRunner implements TransactionRunner {
        boolean failDuringTransaction = false;

        @Override
        public void runInTransaction(Runnable runnable) {
            if (failDuringTransaction) {
                throw new RuntimeException("Simulated transaction rollback");
            }
            runnable.run();
        }

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            if (failDuringTransaction) {
                throw new RuntimeException("Simulated transaction rollback");
            }
            return action.get();
        }
    }
}
