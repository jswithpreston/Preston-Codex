package com.preston.codex.domain.tasks.repository;

import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskOccurrence;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface TaskRepository {

    long insertTask(Task task);

    void updateTask(Task task);

    Task findTaskById(long id);

    int deleteTask(long taskId);

    List<Task> findOneOffTasksByScheduledDate(LocalDate date);

    List<Task> findAllOneOffTasks();

    List<Task> findOverdueOneOffTasks(LocalDate today);

    List<Task> findAllSeriesTasks();

    Set<String> getTaskTags(long taskId);

    void setTaskTags(long taskId, Set<String> tags);

    long insertRecurrenceRule(RecurrenceRule rule);

    void updateRecurrenceRule(RecurrenceRule rule);

    RecurrenceRule findRuleById(long id);

    RecurrenceRule findRuleByTaskId(long taskId);

    List<RecurrenceRule> findAllRules();

    long insertOccurrence(TaskOccurrence occurrence);

    void insertOccurrences(List<TaskOccurrence> occurrences);

    void updateOccurrence(TaskOccurrence occurrence);

    TaskOccurrence findOccurrenceById(long id);

    TaskOccurrence findOccurrenceByTaskIdAndDate(long taskId, LocalDate date);

    List<TaskOccurrence> findOccurrencesByRuleId(long ruleId);

    List<TaskOccurrence> findOccurrencesByTaskId(long taskId);

    List<TaskOccurrence> findOccurrencesByEffectiveDate(LocalDate date);

    List<TaskOccurrence> findMissedOccurrences(LocalDate today);

    int deleteUntouchedFutureOccurrences(long ruleId, LocalDate fromDate);

    int countOccurrencesByRuleId(long ruleId);

    void insertTaskEvent(TaskEvent event);

    List<TaskEvent> findTaskEventsByTaskId(long taskId);

    List<TaskEvent> findTaskEventsByOccurrenceId(long occurrenceId);

    void insertRescheduleRecord(RescheduleRecord record);

    List<RescheduleRecord> findRescheduleRecordsByTaskId(long taskId);

    List<RescheduleRecord> findRescheduleRecordsByOccurrenceId(long occurrenceId);

    void deleteRemindersByOwner(String ownerType, long ownerId);
}
