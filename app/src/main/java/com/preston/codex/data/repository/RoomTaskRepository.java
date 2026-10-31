package com.preston.codex.data.repository;

import com.preston.codex.data.dao.RecurrenceRuleDao;
import com.preston.codex.data.dao.ReminderStateDao;
import com.preston.codex.data.dao.RescheduleHistoryDao;
import com.preston.codex.data.dao.TaskDao;
import com.preston.codex.data.dao.TaskEventDao;
import com.preston.codex.data.dao.TaskOccurrenceDao;
import com.preston.codex.data.dao.TaskTagDao;
import com.preston.codex.data.entity.RecurrenceRuleEntity;
import com.preston.codex.data.entity.RescheduleHistoryEntity;
import com.preston.codex.data.entity.TaskEntity;
import com.preston.codex.data.entity.TaskEventEntity;
import com.preston.codex.data.entity.TaskOccurrenceEntity;
import com.preston.codex.data.entity.TaskTagEntity;
import com.preston.codex.data.mapper.TaskMapper;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.repository.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RoomTaskRepository implements TaskRepository {

    private final TaskDao taskDao;
    private final TaskTagDao taskTagDao;
    private final RecurrenceRuleDao recurrenceRuleDao;
    private final TaskOccurrenceDao taskOccurrenceDao;
    private final TaskEventDao taskEventDao;
    private final RescheduleHistoryDao rescheduleHistoryDao;
    private final ReminderStateDao reminderStateDao;

    public RoomTaskRepository(
            TaskDao taskDao,
            TaskTagDao taskTagDao,
            RecurrenceRuleDao recurrenceRuleDao,
            TaskOccurrenceDao taskOccurrenceDao,
            TaskEventDao taskEventDao,
            RescheduleHistoryDao rescheduleHistoryDao,
            ReminderStateDao reminderStateDao
    ) {
        this.taskDao = Objects.requireNonNull(taskDao, "taskDao must not be null");
        this.taskTagDao = Objects.requireNonNull(taskTagDao, "taskTagDao must not be null");
        this.recurrenceRuleDao = Objects.requireNonNull(recurrenceRuleDao, "recurrenceRuleDao must not be null");
        this.taskOccurrenceDao = Objects.requireNonNull(taskOccurrenceDao, "taskOccurrenceDao must not be null");
        this.taskEventDao = Objects.requireNonNull(taskEventDao, "taskEventDao must not be null");
        this.rescheduleHistoryDao = Objects.requireNonNull(rescheduleHistoryDao, "rescheduleHistoryDao must not be null");
        this.reminderStateDao = Objects.requireNonNull(reminderStateDao, "reminderStateDao must not be null");
    }

    @Override
    public long insertTask(Task task) {
        TaskEntity entity = TaskMapper.toEntity(task);
        long id = taskDao.insert(entity);
        if (task.tags() != null && !task.tags().isEmpty()) {
            setTaskTags(id, task.tags());
        }
        return id;
    }

    @Override
    public void updateTask(Task task) {
        TaskEntity entity = TaskMapper.toEntity(task);
        taskDao.update(entity);
        if (task.tags() != null) {
            setTaskTags(task.id(), task.tags());
        }
    }

    @Override
    public Task findTaskById(long id) {
        TaskEntity entity = taskDao.findById(id);
        if (entity == null) {
            return null;
        }
        Set<String> tags = getTaskTags(id);
        return TaskMapper.toDomain(entity, tags);
    }

    @Override
    public int deleteTask(long taskId) {
        // Explicitly remove reminder_state rows for this task owner in same operation
        reminderStateDao.deleteByOwner("TASK", taskId);
        return taskDao.deleteById(taskId);
    }

    @Override
    public List<Task> findOneOffTasksByScheduledDate(LocalDate date) {
        List<TaskEntity> entities = taskDao.findOneOffTasksByScheduledDate(date.toString());
        List<Task> result = new ArrayList<>(entities.size());
        for (TaskEntity entity : entities) {
            Set<String> tags = getTaskTags(entity.id);
            result.add(TaskMapper.toDomain(entity, tags));
        }
        return result;
    }

    @Override
    public List<Task> findAllOneOffTasks() {
        List<TaskEntity> entities = taskDao.findAllOneOffTasks();
        List<Task> result = new ArrayList<>(entities.size());
        for (TaskEntity entity : entities) {
            Set<String> tags = getTaskTags(entity.id);
            result.add(TaskMapper.toDomain(entity, tags));
        }
        return result;
    }

    @Override
    public List<Task> findOverdueOneOffTasks(LocalDate today) {
        List<TaskEntity> entities = taskDao.findOverdueOneOffTasks(today.toString());
        List<Task> result = new ArrayList<>(entities.size());
        for (TaskEntity entity : entities) {
            Set<String> tags = getTaskTags(entity.id);
            result.add(TaskMapper.toDomain(entity, tags));
        }
        return result;
    }

    @Override
    public List<Task> findAllSeriesTasks() {
        List<TaskEntity> entities = taskDao.findAllSeriesTasks();
        List<Task> result = new ArrayList<>(entities.size());
        for (TaskEntity entity : entities) {
            Set<String> tags = getTaskTags(entity.id);
            result.add(TaskMapper.toDomain(entity, tags));
        }
        return result;
    }

    @Override
    public Set<String> getTaskTags(long taskId) {
        List<String> tags = taskTagDao.findTagsByTaskId(taskId);
        return new HashSet<>(tags);
    }

    @Override
    public void setTaskTags(long taskId, Set<String> tags) {
        taskTagDao.deleteByTaskId(taskId);
        if (tags != null && !tags.isEmpty()) {
            List<TaskTagEntity> entities = new ArrayList<>(tags.size());
            for (String tag : tags) {
                if (tag != null && !tag.trim().isEmpty()) {
                    entities.add(new TaskTagEntity(taskId, tag.trim().toLowerCase()));
                }
            }
            if (!entities.isEmpty()) {
                taskTagDao.insertAll(entities);
            }
        }
    }

    @Override
    public long insertRecurrenceRule(RecurrenceRule rule) {
        return recurrenceRuleDao.insert(TaskMapper.toEntity(rule));
    }

    @Override
    public void updateRecurrenceRule(RecurrenceRule rule) {
        recurrenceRuleDao.update(TaskMapper.toEntity(rule));
    }

    @Override
    public RecurrenceRule findRuleById(long id) {
        RecurrenceRuleEntity entity = recurrenceRuleDao.findById(id);
        return TaskMapper.toDomain(entity);
    }

    @Override
    public RecurrenceRule findRuleByTaskId(long taskId) {
        RecurrenceRuleEntity entity = recurrenceRuleDao.findByTaskId(taskId);
        return TaskMapper.toDomain(entity);
    }

    @Override
    public List<RecurrenceRule> findAllRules() {
        List<RecurrenceRuleEntity> entities = recurrenceRuleDao.findAll();
        List<RecurrenceRule> result = new ArrayList<>(entities.size());
        for (RecurrenceRuleEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public long insertOccurrence(TaskOccurrence occurrence) {
        return taskOccurrenceDao.insert(TaskMapper.toEntity(occurrence));
    }

    @Override
    public void insertOccurrences(List<TaskOccurrence> occurrences) {
        if (occurrences == null || occurrences.isEmpty()) {
            return;
        }
        List<TaskOccurrenceEntity> entities = new ArrayList<>(occurrences.size());
        for (TaskOccurrence occurrence : occurrences) {
            entities.add(TaskMapper.toEntity(occurrence));
        }
        taskOccurrenceDao.insertAll(entities);
    }

    @Override
    public void updateOccurrence(TaskOccurrence occurrence) {
        taskOccurrenceDao.update(TaskMapper.toEntity(occurrence));
    }

    @Override
    public TaskOccurrence findOccurrenceById(long id) {
        TaskOccurrenceEntity entity = taskOccurrenceDao.findById(id);
        return TaskMapper.toDomain(entity);
    }

    @Override
    public TaskOccurrence findOccurrenceByTaskIdAndDate(long taskId, LocalDate date) {
        TaskOccurrenceEntity entity = taskOccurrenceDao.findByTaskIdAndOccurrenceDate(taskId, date.toString());
        return TaskMapper.toDomain(entity);
    }

    @Override
    public List<TaskOccurrence> findOccurrencesByRuleId(long ruleId) {
        List<TaskOccurrenceEntity> entities = taskOccurrenceDao.findByRuleId(ruleId);
        List<TaskOccurrence> result = new ArrayList<>(entities.size());
        for (TaskOccurrenceEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public List<TaskOccurrence> findOccurrencesByTaskId(long taskId) {
        List<TaskOccurrenceEntity> entities = taskOccurrenceDao.findByTaskId(taskId);
        List<TaskOccurrence> result = new ArrayList<>(entities.size());
        for (TaskOccurrenceEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public List<TaskOccurrence> findOccurrencesByEffectiveDate(LocalDate date) {
        List<TaskOccurrenceEntity> entities = taskOccurrenceDao.findByEffectiveDate(date.toString());
        List<TaskOccurrence> result = new ArrayList<>(entities.size());
        for (TaskOccurrenceEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public List<TaskOccurrence> findMissedOccurrences(LocalDate today) {
        List<TaskOccurrenceEntity> entities = taskOccurrenceDao.findMissed(today.toString());
        List<TaskOccurrence> result = new ArrayList<>(entities.size());
        for (TaskOccurrenceEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public int deleteUntouchedFutureOccurrences(long ruleId, LocalDate fromDate) {
        return taskOccurrenceDao.deleteUntouchedFuture(ruleId, fromDate.toString());
    }

    @Override
    public int countOccurrencesByRuleId(long ruleId) {
        return taskOccurrenceDao.countByRuleId(ruleId);
    }

    @Override
    public void insertTaskEvent(TaskEvent event) {
        taskEventDao.insert(TaskMapper.toEntity(event));
    }

    @Override
    public List<TaskEvent> findTaskEventsByTaskId(long taskId) {
        List<TaskEventEntity> entities = taskEventDao.findByTaskId(taskId);
        List<TaskEvent> result = new ArrayList<>(entities.size());
        for (TaskEventEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public List<TaskEvent> findTaskEventsByOccurrenceId(long occurrenceId) {
        List<TaskEventEntity> entities = taskEventDao.findByOccurrenceId(occurrenceId);
        List<TaskEvent> result = new ArrayList<>(entities.size());
        for (TaskEventEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public void insertRescheduleRecord(RescheduleRecord record) {
        rescheduleHistoryDao.insert(TaskMapper.toEntity(record));
    }

    @Override
    public List<RescheduleRecord> findRescheduleRecordsByTaskId(long taskId) {
        List<RescheduleHistoryEntity> entities = rescheduleHistoryDao.findByTaskId(taskId);
        List<RescheduleRecord> result = new ArrayList<>(entities.size());
        for (RescheduleHistoryEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public List<RescheduleRecord> findRescheduleRecordsByOccurrenceId(long occurrenceId) {
        List<RescheduleHistoryEntity> entities = rescheduleHistoryDao.findByOccurrenceId(occurrenceId);
        List<RescheduleRecord> result = new ArrayList<>(entities.size());
        for (RescheduleHistoryEntity entity : entities) {
            result.add(TaskMapper.toDomain(entity));
        }
        return result;
    }

    @Override
    public void deleteRemindersByOwner(String ownerType, long ownerId) {
        reminderStateDao.deleteByOwner(ownerType, ownerId);
    }
}
