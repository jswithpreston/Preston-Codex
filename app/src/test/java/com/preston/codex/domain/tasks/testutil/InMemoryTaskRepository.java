package com.preston.codex.domain.tasks.testutil;

import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.repository.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryTaskRepository implements TaskRepository {
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
        res.sort((a, b) -> a.occurrenceDate().compareTo(b.occurrenceDate()));
        return res;
    }

    @Override
    public List<TaskOccurrence> findOccurrencesByTaskId(long taskId) {
        List<TaskOccurrence> res = new ArrayList<>();
        for (TaskOccurrence o : occurrences.values()) {
            if (o.taskId() == taskId) res.add(o);
        }
        res.sort((a, b) -> a.occurrenceDate().compareTo(b.occurrenceDate()));
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
