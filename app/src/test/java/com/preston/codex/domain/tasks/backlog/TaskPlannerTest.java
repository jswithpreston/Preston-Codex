package com.preston.codex.domain.tasks.backlog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskPriority;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskPlannerTest {

    private LocalDate today;

    @Before
    public void setUp() {
        today = LocalDate.of(2026, 5, 1);
    }

    private BacklogCandidate candidate(long id, String title, TaskPriority priority, LocalDate originalDate, LocalDate deadline, int duration, int rescheduleCount, boolean isFlexible) {
        return new BacklogCandidate(
                id,
                TaskKind.ONE_OFF,
                title,
                priority,
                originalDate,
                deadline,
                duration,
                rescheduleCount,
                isFlexible
        );
    }

    @Test
    public void plan_respectsDailyCapOfSixTasks() {
        // Today has 6 tasks already
        Map<LocalDate, TaskPlanner.DayLoad> initialLoads = new HashMap<>();
        initialLoads.put(today, new TaskPlanner.DayLoad(6, 120));

        BacklogCandidate c = candidate(1L, "Task 1", TaskPriority.MEDIUM, today.minusDays(1), null, 30, 0, true);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, initialLoads);

        assertEquals(1, preview.proposedMoves().size());
        // Spilled to tomorrow
        assertEquals(today.plusDays(1), preview.proposedMoves().get(0).toDate());
    }

    @Test
    public void plan_respectsDailyCapOf240Minutes() {
        // Today has 3 tasks totaling 220 minutes
        Map<LocalDate, TaskPlanner.DayLoad> initialLoads = new HashMap<>();
        initialLoads.put(today, new TaskPlanner.DayLoad(3, 220));

        // Candidate needs 30 mins -> 220 + 30 = 250 > 240, must spill to tomorrow
        BacklogCandidate c = candidate(1L, "Task 1", TaskPriority.MEDIUM, today.minusDays(1), null, 30, 0, true);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, initialLoads);

        assertEquals(1, preview.proposedMoves().size());
        assertEquals(today.plusDays(1), preview.proposedMoves().get(0).toDate());
    }

    @Test
    public void plan_usesDefault30MinutesWhenNullOrZeroDuration() {
        // Day has 220 minutes, 1 task with 0 duration defaults to 30 mins, exceeding 240
        Map<LocalDate, TaskPlanner.DayLoad> initialLoads = new HashMap<>();
        initialLoads.put(today, new TaskPlanner.DayLoad(1, 220));

        BacklogCandidate c = candidate(1L, "Task with 0 dur", TaskPriority.LOW, today.minusDays(1), null, 0, 0, true);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, initialLoads);

        assertEquals(today.plusDays(1), preview.proposedMoves().get(0).toDate());
        assertEquals(30, preview.proposedMoves().get(0).durationMinutes());
    }

    @Test
    public void plan_sortingOrder_prioritizesUrgentAndEarlierDeadlines() {
        BacklogCandidate lowUrgent = candidate(1L, "Low priority", TaskPriority.LOW, today.minusDays(1), today.plusDays(5), 30, 0, true);
        BacklogCandidate highUrgent = candidate(2L, "High priority", TaskPriority.URGENT, today.minusDays(1), today.plusDays(5), 30, 0, true);

        // Pre-fill today so only 1 slot is left
        Map<LocalDate, TaskPlanner.DayLoad> initialLoads = new HashMap<>();
        initialLoads.put(today, new TaskPlanner.DayLoad(5, 150));

        List<BacklogCandidate> candidates = new ArrayList<>();
        candidates.add(lowUrgent);
        candidates.add(highUrgent);

        BacklogReviewPreview preview = TaskPlanner.plan(candidates, today, initialLoads);
        assertEquals(2, preview.proposedMoves().size());

        // URGENT gets today
        assertEquals(2L, preview.proposedMoves().get(0).id());
        assertEquals(today, preview.proposedMoves().get(0).toDate());

        // LOW gets spilled to tomorrow
        assertEquals(1L, preview.proposedMoves().get(1).id());
        assertEquals(today.plusDays(1), preview.proposedMoves().get(1).toDate());
    }

    @Test
    public void plan_deadlineBoundary_deadlineEqualsToday_canBePlacedToday() {
        // Amendment 11 requirement: boundary case deadline == today
        BacklogCandidate c = candidate(1L, "Due Today", TaskPriority.MEDIUM, today.minusDays(2), today, 30, 0, true);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, Collections.emptyMap());

        assertEquals(1, preview.proposedMoves().size());
        assertEquals(today, preview.proposedMoves().get(0).toDate());
        assertTrue(preview.needsAttention().isEmpty());
    }

    @Test
    public void plan_deadlineBoundary_deadlineEqualsTargetDay_canBePlacedOnTargetDayOnly() {
        // Amendment 11 requirement: boundary case deadline == target day
        LocalDate targetDay = today.plusDays(2);
        // Fill today and today+1 completely (6 tasks each)
        Map<LocalDate, TaskPlanner.DayLoad> initialLoads = new HashMap<>();
        initialLoads.put(today, new TaskPlanner.DayLoad(6, 180));
        initialLoads.put(today.plusDays(1), new TaskPlanner.DayLoad(6, 180));

        BacklogCandidate c = candidate(1L, "Target Deadline", TaskPriority.HIGH, today.minusDays(1), targetDay, 30, 0, true);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, initialLoads);

        assertEquals(1, preview.proposedMoves().size());
        assertEquals(targetDay, preview.proposedMoves().get(0).toDate());

        // If targetDay is ALSO full, it cannot be placed after targetDay and goes to needsAttention!
        initialLoads.put(targetDay, new TaskPlanner.DayLoad(6, 180));
        BacklogReviewPreview previewFull = TaskPlanner.plan(Collections.singletonList(c), today, initialLoads);
        assertTrue(previewFull.proposedMoves().isEmpty());
        assertEquals(1, previewFull.needsAttention().size());
        assertEquals(1L, previewFull.needsAttention().get(0).id());
    }

    @Test
    public void plan_deadlineInPast_goesToNeedsAttention() {
        BacklogCandidate c = candidate(1L, "Past Deadline", TaskPriority.HIGH, today.minusDays(5), today.minusDays(1), 30, 0, true);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, Collections.emptyMap());

        assertTrue(preview.proposedMoves().isEmpty());
        assertEquals(1, preview.needsAttention().size());
        assertEquals(1L, preview.needsAttention().get(0).id());
    }

    @Test
    public void plan_nonFlexibleTasks_areSkipped() {
        BacklogCandidate c = candidate(1L, "Rigid Task", TaskPriority.HIGH, today.minusDays(2), today.plusDays(5), 30, 0, false);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, Collections.emptyMap());

        assertTrue(preview.proposedMoves().isEmpty());
        assertTrue(preview.needsAttention().isEmpty());
    }

    @Test
    public void plan_exceedingMaxReschedules_goesToNeedsAttention() {
        BacklogCandidate c = candidate(1L, "Chronic Task", TaskPriority.MEDIUM, today.minusDays(2), today.plusDays(5), 30, 3, true);
        BacklogReviewPreview preview = TaskPlanner.plan(Collections.singletonList(c), today, Collections.emptyMap());

        assertTrue(preview.proposedMoves().isEmpty());
        assertEquals(1, preview.needsAttention().size());
        assertEquals(1L, preview.needsAttention().get(0).id());
    }
}
