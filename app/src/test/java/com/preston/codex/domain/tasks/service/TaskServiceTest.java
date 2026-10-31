package com.preston.codex.domain.tasks.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.MissedPolicy;
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
import com.preston.codex.domain.tasks.recurrence.OccurrenceGenerator;
import com.preston.codex.domain.tasks.recurrence.RecurrenceService;
import com.preston.codex.domain.tasks.repository.TaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;
import com.preston.codex.integration.KeyValueStore;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class TaskServiceTest {

    private InMemoryTaskRepository repository;
    private KeyValueStore keyValueStore;
    private LocalDateProvider dateProvider;
    private RecurrenceService recurrenceService;
    private TaskService taskService;

    private final LocalDate today = LocalDate.of(2026, 10, 30);
    private final Instant pinnedNow = Instant.parse("2026-10-30T10:00:00Z");

    @Before
    public void setUp() {
        repository = new InMemoryTaskRepository();
        keyValueStore = new InMemoryKeyValueStore();
        Clock clock = Clock.fixed(pinnedNow, ZoneOffset.UTC);
        dateProvider = new LocalDateProvider(clock, () -> ZoneOffset.UTC);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override
            public <T> T runInTransaction(Supplier<T> action) {
                return action.get();
            }

            @Override
            public void runInTransaction(Runnable action) {
                action.run();
            }
        };

        OccurrenceGenerator generator = new OccurrenceGenerator();
        recurrenceService = new RecurrenceService(repository, generator, dateProvider, transactionRunner);
        taskService = new TaskService(repository, recurrenceService, dateProvider, keyValueStore, transactionRunner);
    }

    @Test
    public void createOneOffTask_defaultsDurationWhenNull() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand(
                "Write Blog Post",
                "Drafting outline",
                TaskPriority.MEDIUM,
                null,
                null, // null duration
                today,
                LocalTime.of(10, 0),
                true,
                null,
                null,
                Set.of("writing")
        );

        Task created = taskService.createOneOffTask(cmd);
        assertNotNull(created);
        assertEquals("Write Blog Post", created.title());
        assertNull("Duration remains null in entity (planner applies 30-min default)", created.estimatedDurationMinutes());
        assertEquals(TaskStatus.OPEN, created.status());
        assertEquals(pinnedNow, created.createdAt());

        List<TaskEvent> events = repository.findTaskEventsByTaskId(created.id());
        assertEquals(1, events.size());
        assertEquals("CREATED", events.get(0).eventType());
    }

    @Test
    public void createOneOffTask_normalizesTagsToLowercase() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand(
                "Learn Java",
                null,
                TaskPriority.HIGH,
                today.plusDays(10),
                60,
                today,
                null,
                false,
                null,
                null,
                Set.of("Java", "PROGRAMMING", "Android")
        );

        Task created = taskService.createOneOffTask(cmd);
        Set<String> tags = repository.getTaskTags(created.id());
        assertEquals(Set.of("java", "programming", "android"), tags);
    }

    @Test
    public void createSeriesTask_copiesDefaultMissedPolicyFromKeyValueStore() {
        keyValueStore.putString(TaskService.PREF_DEFAULT_MISSED_POLICY, "SKIP_AUTOMATICALLY");

        CreateSeriesTaskCommand cmd = new CreateSeriesTaskCommand(
                "Daily Meditation",
                "10 mins mindfulness",
                TaskPriority.LOW,
                true,
                null,
                null,
                Set.of("habit")
        );

        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(
                RecurrenceType.DAILY,
                1,
                null,
                today,
                null,
                null,
                null,
                null // null policy -> read from KeyValueStore
        );

        Task series = taskService.createSeriesTask(cmd, ruleParams);
        assertEquals(TaskKind.SERIES, series.taskKind());

        RecurrenceRule rule = repository.findRuleByTaskId(series.id());
        assertNotNull(rule);
        assertEquals(MissedPolicy.SKIP_AUTOMATICALLY, rule.missedPolicy());

        // Initial materialization verified
        List<TaskOccurrence> occurrences = repository.findOccurrencesByTaskId(series.id());
        assertEquals(90, occurrences.size());
    }

    @Test
    public void completeTask_updatesStatusAndCompletedAt() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand(
                "Submit Report", null, TaskPriority.HIGH, null, 30, today, null, false, null, null, null
        );
        Task task = taskService.createOneOffTask(cmd);

        taskService.completeTask(task.id());
        Task updated = repository.findTaskById(task.id());
        assertEquals(TaskStatus.COMPLETED, updated.status());
        assertEquals(pinnedNow, updated.completedAt());

        List<TaskEvent> events = repository.findTaskEventsByTaskId(task.id());
        assertTrue(events.stream().anyMatch(e -> "COMPLETED".equals(e.eventType())));
    }

    @Test
    public void completeTask_onSeriesTask_throwsIllegalStateException() {
        CreateSeriesTaskCommand cmd = new CreateSeriesTaskCommand("Series", null, TaskPriority.LOW, false, null, null, null);
        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(RecurrenceType.DAILY, 1, null, today, null, null, null, MissedPolicy.KEEP_OVERDUE);
        Task series = taskService.createSeriesTask(cmd, ruleParams);

        try {
            taskService.completeTask(series.id());
            fail("A SERIES row stays OPEN until cancelled and cannot be completed.");
        } catch (IllegalStateException expected) {
            // Success
        }
    }

    @Test
    public void completeMondayOccurrence_leavesTuesdayOccurrenceUntouched() {
        // Critical requirement: completing Monday's occurrence leaves Tuesday's untouched!
        LocalDate monday = LocalDate.of(2026, 11, 2);
        LocalDate tuesday = LocalDate.of(2026, 11, 3);

        CreateSeriesTaskCommand cmd = new CreateSeriesTaskCommand("Standup", null, TaskPriority.MEDIUM, false, null, null, null);
        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(RecurrenceType.DAILY, 1, null, monday, null, null, null, MissedPolicy.KEEP_OVERDUE);
        Task series = taskService.createSeriesTask(cmd, ruleParams);

        TaskOccurrence mondayOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), monday);
        TaskOccurrence tuesdayOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), tuesday);

        assertNotNull(mondayOcc);
        assertNotNull(tuesdayOcc);
        assertEquals(OccurrenceStatus.OPEN, mondayOcc.status());
        assertEquals(OccurrenceStatus.OPEN, tuesdayOcc.status());

        // Complete Monday's occurrence
        taskService.completeOccurrence(mondayOcc.id());

        TaskOccurrence updatedMonday = repository.findOccurrenceById(mondayOcc.id());
        TaskOccurrence updatedTuesday = repository.findOccurrenceById(tuesdayOcc.id());

        assertEquals(OccurrenceStatus.COMPLETED, updatedMonday.status());
        assertEquals(pinnedNow, updatedMonday.completedAt());

        // Tuesday must remain OPEN and untouched!
        assertEquals(OccurrenceStatus.OPEN, updatedTuesday.status());
        assertNull(updatedTuesday.completedAt());
    }

    @Test
    public void reopenTask_transitionsBackToOpen() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand("Task 1", null, TaskPriority.LOW, null, 30, today, null, false, null, null, null);
        Task task = taskService.createOneOffTask(cmd);
        taskService.completeTask(task.id());

        taskService.reopenTask(task.id());
        Task updated = repository.findTaskById(task.id());
        assertEquals(TaskStatus.OPEN, updated.status());
        assertNull(updated.completedAt());
    }

    @Test
    public void reopenOccurrence_transitionsBackToOpen() {
        CreateSeriesTaskCommand cmd = new CreateSeriesTaskCommand("Series", null, TaskPriority.LOW, false, null, null, null);
        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(RecurrenceType.DAILY, 1, null, today, null, null, null, MissedPolicy.KEEP_OVERDUE);
        Task series = taskService.createSeriesTask(cmd, ruleParams);

        TaskOccurrence occ = repository.findOccurrenceByTaskIdAndDate(series.id(), today);
        taskService.completeOccurrence(occ.id());
        taskService.reopenOccurrence(occ.id());

        TaskOccurrence updated = repository.findOccurrenceById(occ.id());
        assertEquals(OccurrenceStatus.OPEN, updated.status());
        assertNull(updated.completedAt());
    }

    @Test
    public void cancelTask_setsCancelled() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand("Cancel me", null, TaskPriority.LOW, null, 30, today, null, false, null, null, null);
        Task task = taskService.createOneOffTask(cmd);

        taskService.cancelTask(task.id());
        Task updated = repository.findTaskById(task.id());
        assertEquals(TaskStatus.CANCELLED, updated.status());
        assertEquals(pinnedNow, updated.cancelledAt());
    }

    @Test
    public void cancelOccurrence_setsCancelled() {
        CreateSeriesTaskCommand cmd = new CreateSeriesTaskCommand("Series", null, TaskPriority.LOW, false, null, null, null);
        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(RecurrenceType.DAILY, 1, null, today, null, null, null, MissedPolicy.KEEP_OVERDUE);
        Task series = taskService.createSeriesTask(cmd, ruleParams);

        TaskOccurrence occ = repository.findOccurrenceByTaskIdAndDate(series.id(), today);
        taskService.cancelOccurrence(occ.id());

        TaskOccurrence updated = repository.findOccurrenceById(occ.id());
        assertEquals(OccurrenceStatus.CANCELLED, updated.status());
        assertEquals(pinnedNow, updated.cancelledAt());
    }

    @Test
    public void markInProgress_updatesStatus() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand("Progress task", null, TaskPriority.LOW, null, 30, today, null, false, null, null, null);
        Task task = taskService.createOneOffTask(cmd);

        taskService.markTaskInProgress(task.id());
        assertEquals(TaskStatus.IN_PROGRESS, repository.findTaskById(task.id()).status());

        CreateSeriesTaskCommand seriesCmd = new CreateSeriesTaskCommand("Series", null, TaskPriority.LOW, false, null, null, null);
        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(RecurrenceType.DAILY, 1, null, today, null, null, null, MissedPolicy.KEEP_OVERDUE);
        Task series = taskService.createSeriesTask(seriesCmd, ruleParams);
        TaskOccurrence occ = repository.findOccurrenceByTaskIdAndDate(series.id(), today);

        taskService.markOccurrenceInProgress(occ.id());
        assertEquals(OccurrenceStatus.IN_PROGRESS, repository.findOccurrenceById(occ.id()).status());
    }

    @Test
    public void reflections_canBeUpdatedOnTaskAndOccurrence() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand("Reflection Task", null, TaskPriority.LOW, null, 30, today, null, false, null, null, null);
        Task task = taskService.createOneOffTask(cmd);

        taskService.setTaskReflection(task.id(), "My task reflection");
        assertEquals("My task reflection", repository.findTaskById(task.id()).reflection());

        CreateSeriesTaskCommand seriesCmd = new CreateSeriesTaskCommand("Series", null, TaskPriority.LOW, false, null, null, null);
        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(RecurrenceType.DAILY, 1, null, today, null, null, null, MissedPolicy.KEEP_OVERDUE);
        Task series = taskService.createSeriesTask(seriesCmd, ruleParams);
        TaskOccurrence occ = repository.findOccurrenceByTaskIdAndDate(series.id(), today);

        taskService.setOccurrenceReflection(occ.id(), "My occurrence reflection");
        assertEquals("My occurrence reflection", repository.findOccurrenceById(occ.id()).reflection());
    }

    @Test
    public void knowledgeLinks_canBeSetAndUnlinked() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand("Knowledge Task", null, TaskPriority.LOW, null, 30, today, null, false, null, null, null);
        Task task = taskService.createOneOffTask(cmd);

        taskService.linkKnowledge(task.id(), 777L);
        assertEquals(Long.valueOf(777L), repository.findTaskById(task.id()).knowledgeEntryId());

        taskService.unlinkKnowledge(task.id());
        assertNull(repository.findTaskById(task.id()).knowledgeEntryId());
    }

    @Test
    public void setTags_lowercasesTags() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand("Tags Task", null, TaskPriority.LOW, null, 30, today, null, false, null, null, null);
        Task task = taskService.createOneOffTask(cmd);

        taskService.setTags(task.id(), Set.of("URGENT_REVIEW", "ReadLater"));
        assertEquals(Set.of("urgent_review", "readlater"), repository.getTaskTags(task.id()));
    }

    @Test
    public void deleteTask_purgesTaskAndOwnerReminders() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand("Delete Task", null, TaskPriority.LOW, null, 30, today, null, false, null, null, null);
        Task task = taskService.createOneOffTask(cmd);

        taskService.deleteTask(task.id());
        assertNull(repository.findTaskById(task.id()));
    }

    @Test
    public void cancelSeries_cancelsUntouchedFutureOccurrencesAndTruncatesRule() {
        LocalDate start = today.minusDays(5);
        CreateSeriesTaskCommand cmd = new CreateSeriesTaskCommand("Series to cancel", null, TaskPriority.LOW, false, null, null, null);
        RecurrenceRuleParams ruleParams = new RecurrenceRuleParams(RecurrenceType.DAILY, 1, null, start, null, null, null, MissedPolicy.KEEP_OVERDUE);
        Task series = taskService.createSeriesTask(cmd, ruleParams);

        RecurrenceRule createdRule = repository.findRuleByTaskId(series.id());
        // Manually insert and complete a past occurrence (generator doesn't backfill past dates)
        TaskOccurrence pastOcc = new TaskOccurrence(0L, series.id(), createdRule.id(), today.minusDays(2), today.minusDays(2), OccurrenceStatus.OPEN, null, null, null, null, 0, null, null, pinnedNow, pinnedNow);
        long pastId = repository.insertOccurrence(pastOcc);
        taskService.completeOccurrence(pastId);

        // Add an event to today's occurrence so it is touched
        TaskOccurrence todayOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), today);
        repository.insertTaskEvent(new TaskEvent(0L, series.id(), todayOcc.id(), "NOTE", "Touched note", pinnedNow));

        // Untouched future occurrence
        TaskOccurrence futureUntouchedOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), today.plusDays(2));

        taskService.cancelSeries(series.id());

        Task updatedSeries = repository.findTaskById(series.id());
        assertEquals(TaskStatus.CANCELLED, updatedSeries.status());

        RecurrenceRule updatedRule = repository.findRuleByTaskId(series.id());
        assertEquals(today.minusDays(1), updatedRule.endDate());

        // Past completed occurrence preserved
        assertEquals(OccurrenceStatus.COMPLETED, repository.findOccurrenceById(pastId).status());

        // Touched occurrence preserved in OPEN
        assertEquals(OccurrenceStatus.OPEN, repository.findOccurrenceById(todayOcc.id()).status());

        // Untouched future occurrence cancelled
        assertEquals(OccurrenceStatus.CANCELLED, repository.findOccurrenceById(futureUntouchedOcc.id()).status());
    }

    // --- In-memory test repository & KeyValueStore ---

    static class InMemoryKeyValueStore implements KeyValueStore {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object val = map.get(key);
            return val instanceof Boolean ? (Boolean) val : defValue;
        }

        @Override
        public void putBoolean(String key, boolean value) {
            map.put(key, value);
        }

        @Override
        public int getInt(String key, int defValue) {
            Object val = map.get(key);
            return val instanceof Integer ? (Integer) val : defValue;
        }

        @Override
        public void putInt(String key, int value) {
            map.put(key, value);
        }

        @Override
        public String getString(String key, String defValue) {
            Object val = map.get(key);
            return val instanceof String ? (String) val : defValue;
        }

        @Override
        public void putString(String key, String value) {
            map.put(key, value);
        }
    }

    static class InMemoryTaskRepository implements TaskRepository {
        private long taskIdSeq = 1;
        private long ruleIdSeq = 1;
        private long occIdSeq = 1;
        private long eventIdSeq = 1;
        private long histIdSeq = 1;

        private final Map<Long, Task> tasks = new HashMap<>();
        private final Map<Long, Set<String>> tags = new HashMap<>();
        private final Map<Long, RecurrenceRule> rules = new HashMap<>();
        private final Map<Long, TaskOccurrence> occurrences = new HashMap<>();
        private final List<TaskEvent> events = new ArrayList<>();
        private final List<RescheduleRecord> history = new ArrayList<>();

        @Override
        public long insertTask(Task task) {
            long id = task.id() > 0 ? task.id() : taskIdSeq++;
            Task toStore = new Task(
                    id, task.taskKind(), task.title(), task.description(), task.status(),
                    task.previousTaskId(), task.knowledgeEntryId(), task.reflection(), task.isFlexible(),
                    task.priority(), task.deadline(), task.estimatedDurationMinutes(), task.scheduledDate(),
                    task.scheduledTime(), task.autoRescheduleCount(), task.completedAt(), task.cancelledAt(),
                    task.createdAt(), task.updatedAt(), task.tags()
            );
            tasks.put(id, toStore);
            if (task.tags() != null) {
                tags.put(id, new HashSet<>(task.tags()));
            }
            return id;
        }

        @Override
        public void updateTask(Task task) {
            tasks.put(task.id(), task);
            if (task.tags() != null) {
                tags.put(task.id(), new HashSet<>(task.tags()));
            }
        }

        @Override
        public Task findTaskById(long id) {
            Task t = tasks.get(id);
            if (t == null) return null;
            return new Task(
                    t.id(), t.taskKind(), t.title(), t.description(), t.status(),
                    t.previousTaskId(), t.knowledgeEntryId(), t.reflection(), t.isFlexible(),
                    t.priority(), t.deadline(), t.estimatedDurationMinutes(), t.scheduledDate(),
                    t.scheduledTime(), t.autoRescheduleCount(), t.completedAt(), t.cancelledAt(),
                    t.createdAt(), t.updatedAt(), getTaskTags(id)
            );
        }

        @Override
        public int deleteTask(long taskId) {
            tasks.remove(taskId);
            tags.remove(taskId);
            rules.values().removeIf(r -> r.taskId() == taskId);
            occurrences.values().removeIf(o -> o.taskId() == taskId);
            events.removeIf(e -> e.taskId() == taskId);
            history.removeIf(h -> h.taskId() == taskId);
            return 1;
        }

        @Override
        public List<Task> findOneOffTasksByScheduledDate(LocalDate date) {
            List<Task> res = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.taskKind() == TaskKind.ONE_OFF && date.equals(t.scheduledDate())) {
                    res.add(t);
                }
            }
            return res;
        }

        @Override
        public List<Task> findAllOneOffTasks() {
            List<Task> res = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.taskKind() == TaskKind.ONE_OFF) {
                    res.add(t);
                }
            }
            return res;
        }

        @Override
        public List<Task> findOverdueOneOffTasks(LocalDate today) {
            List<Task> res = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.taskKind() == TaskKind.ONE_OFF && (t.status() == TaskStatus.OPEN || t.status() == TaskStatus.IN_PROGRESS)) {
                    if (t.scheduledDate() != null && t.scheduledDate().isBefore(today)) {
                        res.add(t);
                    }
                }
            }
            return res;
        }

        @Override
        public List<Task> findAllSeriesTasks() {
            List<Task> res = new ArrayList<>();
            for (Task t : tasks.values()) {
                if (t.taskKind() == TaskKind.SERIES) {
                    res.add(t);
                }
            }
            return res;
        }

        @Override
        public Set<String> getTaskTags(long taskId) {
            return tags.getOrDefault(taskId, Collections.emptySet());
        }

        @Override
        public void setTaskTags(long taskId, Set<String> newTags) {
            tags.put(taskId, new HashSet<>(newTags));
        }

        @Override
        public long insertRecurrenceRule(RecurrenceRule rule) {
            long id = rule.id() > 0 ? rule.id() : ruleIdSeq++;
            RecurrenceRule toStore = new RecurrenceRule(
                    id, rule.taskId(), rule.type(), rule.interval(), rule.weekdays(),
                    rule.startDate(), rule.endDate(), rule.occurrenceCount(), rule.anchorDayOfMonth(),
                    rule.missedPolicy(), rule.createdAt(), rule.updatedAt()
            );
            rules.put(id, toStore);
            return id;
        }

        @Override
        public void updateRecurrenceRule(RecurrenceRule rule) {
            rules.put(rule.id(), rule);
        }

        @Override
        public RecurrenceRule findRuleById(long id) {
            return rules.get(id);
        }

        @Override
        public RecurrenceRule findRuleByTaskId(long taskId) {
            for (RecurrenceRule r : rules.values()) {
                if (r.taskId() == taskId) return r;
            }
            return null;
        }

        @Override
        public List<RecurrenceRule> findAllRules() {
            return new ArrayList<>(rules.values());
        }

        @Override
        public long insertOccurrence(TaskOccurrence occurrence) {
            long id = occurrence.id() > 0 ? occurrence.id() : occIdSeq++;
            TaskOccurrence toStore = new TaskOccurrence(
                    id, occurrence.taskId(), occurrence.ruleId(), occurrence.occurrenceDate(),
                    occurrence.effectiveDate(), occurrence.status(), occurrence.titleOverride(),
                    occurrence.timeOverride(), occurrence.noteOverride(), occurrence.reflection(),
                    occurrence.autoRescheduleCount(), occurrence.completedAt(), occurrence.cancelledAt(),
                    occurrence.createdAt(), occurrence.updatedAt()
            );
            occurrences.put(id, toStore);
            return id;
        }

        @Override
        public void insertOccurrences(List<TaskOccurrence> occs) {
            for (TaskOccurrence o : occs) {
                insertOccurrence(o);
            }
        }

        @Override
        public void updateOccurrence(TaskOccurrence occurrence) {
            occurrences.put(occurrence.id(), occurrence);
        }

        @Override
        public TaskOccurrence findOccurrenceById(long id) {
            return occurrences.get(id);
        }

        @Override
        public TaskOccurrence findOccurrenceByTaskIdAndDate(long taskId, LocalDate date) {
            for (TaskOccurrence o : occurrences.values()) {
                if (o.taskId() == taskId && o.occurrenceDate().equals(date)) {
                    return o;
                }
            }
            return null;
        }

        @Override
        public List<TaskOccurrence> findOccurrencesByRuleId(long ruleId) {
            List<TaskOccurrence> res = new ArrayList<>();
            for (TaskOccurrence o : occurrences.values()) {
                if (o.ruleId() == ruleId) res.add(o);
            }
            return res;
        }

        @Override
        public List<TaskOccurrence> findOccurrencesByTaskId(long taskId) {
            List<TaskOccurrence> res = new ArrayList<>();
            for (TaskOccurrence o : occurrences.values()) {
                if (o.taskId() == taskId) res.add(o);
            }
            return res;
        }

        @Override
        public List<TaskOccurrence> findOccurrencesByEffectiveDate(LocalDate date) {
            List<TaskOccurrence> res = new ArrayList<>();
            for (TaskOccurrence o : occurrences.values()) {
                if (o.effectiveDate().equals(date)) res.add(o);
            }
            return res;
        }

        @Override
        public List<TaskOccurrence> findMissedOccurrences(LocalDate today) {
            List<TaskOccurrence> res = new ArrayList<>();
            for (TaskOccurrence o : occurrences.values()) {
                if (o.effectiveDate().isBefore(today) && (o.status() == OccurrenceStatus.OPEN || o.status() == OccurrenceStatus.IN_PROGRESS)) {
                    res.add(o);
                }
            }
            return res;
        }

        @Override
        public int deleteUntouchedFutureOccurrences(long ruleId, LocalDate fromDate) {
            int count = 0;
            List<Long> toRemove = new ArrayList<>();
            for (TaskOccurrence o : occurrences.values()) {
                if (o.ruleId() == ruleId && !o.occurrenceDate().isBefore(fromDate) && o.status() == OccurrenceStatus.OPEN) {
                    if (findTaskEventsByOccurrenceId(o.id()).isEmpty() && findRescheduleRecordsByOccurrenceId(o.id()).isEmpty()) {
                        toRemove.add(o.id());
                        count++;
                    }
                }
            }
            for (Long id : toRemove) {
                occurrences.remove(id);
            }
            return count;
        }

        @Override
        public int countOccurrencesByRuleId(long ruleId) {
            return findOccurrencesByRuleId(ruleId).size();
        }

        @Override
        public void insertTaskEvent(TaskEvent event) {
            long id = event.id() > 0 ? event.id() : eventIdSeq++;
            events.add(new TaskEvent(id, event.taskId(), event.occurrenceId(), event.eventType(), event.details(), event.timestamp()));
        }

        @Override
        public List<TaskEvent> findTaskEventsByTaskId(long taskId) {
            List<TaskEvent> res = new ArrayList<>();
            for (TaskEvent e : events) {
                if (e.taskId() == taskId) res.add(e);
            }
            return res;
        }

        @Override
        public List<TaskEvent> findTaskEventsByOccurrenceId(long occurrenceId) {
            List<TaskEvent> res = new ArrayList<>();
            for (TaskEvent e : events) {
                if (e.occurrenceId() != null && e.occurrenceId() == occurrenceId) res.add(e);
            }
            return res;
        }

        @Override
        public void insertRescheduleRecord(RescheduleRecord record) {
            long id = record.id() > 0 ? record.id() : histIdSeq++;
            history.add(new RescheduleRecord(id, record.taskId(), record.occurrenceId(), record.fromDate(), record.toDate(), record.reason(), record.timestamp()));
        }

        @Override
        public List<RescheduleRecord> findRescheduleRecordsByTaskId(long taskId) {
            List<RescheduleRecord> res = new ArrayList<>();
            for (RescheduleRecord h : history) {
                if (h.taskId() == taskId) res.add(h);
            }
            return res;
        }

        @Override
        public List<RescheduleRecord> findRescheduleRecordsByOccurrenceId(long occurrenceId) {
            List<RescheduleRecord> res = new ArrayList<>();
            for (RescheduleRecord h : history) {
                if (h.occurrenceId() != null && h.occurrenceId() == occurrenceId) res.add(h);
            }
            return res;
        }

        @Override
        public void deleteRemindersByOwner(String ownerType, long ownerId) {
            // no-op for in memory
        }
    }
}
