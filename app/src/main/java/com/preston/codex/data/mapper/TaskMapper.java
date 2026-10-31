package com.preston.codex.data.mapper;

import com.preston.codex.data.entity.RecurrenceRuleEntity;
import com.preston.codex.data.entity.ReminderStateEntity;
import com.preston.codex.data.entity.RescheduleHistoryEntity;
import com.preston.codex.data.entity.TaskEntity;
import com.preston.codex.data.entity.TaskEventEntity;
import com.preston.codex.data.entity.TaskOccurrenceEntity;
import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RecurrenceType;
import com.preston.codex.domain.tasks.model.ReminderState;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskPriority;
import com.preston.codex.domain.tasks.model.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskEntity toEntity(Task task) {
        if (task == null) {
            return null;
        }
        TaskEntity entity = new TaskEntity();
        entity.id = task.id();
        entity.taskKind = task.taskKind().name();
        entity.title = task.title();
        entity.description = task.description();
        entity.status = task.status().name();
        entity.previousTaskId = task.previousTaskId();
        entity.knowledgeEntryId = task.knowledgeEntryId();
        entity.reflection = task.reflection();
        entity.isFlexible = task.isFlexible();
        entity.priority = task.priority().name();
        entity.deadline = task.deadline() != null ? task.deadline().toString() : null;
        entity.estimatedDurationMinutes = task.estimatedDurationMinutes();
        entity.scheduledDate = task.scheduledDate() != null ? task.scheduledDate().toString() : null;
        entity.scheduledTime = task.scheduledTime() != null ? task.scheduledTime().toString() : null;
        entity.autoRescheduleCount = task.autoRescheduleCount();
        entity.completedAt = task.completedAt() != null ? task.completedAt().toEpochMilli() : null;
        entity.cancelledAt = task.cancelledAt() != null ? task.cancelledAt().toEpochMilli() : null;
        entity.createdAt = task.createdAt().toEpochMilli();
        entity.updatedAt = task.updatedAt().toEpochMilli();
        return entity;
    }

    public static Task toDomain(TaskEntity entity, Set<String> tags) {
        if (entity == null) {
            return null;
        }
        return new Task(
                entity.id,
                TaskKind.valueOf(entity.taskKind),
                entity.title,
                entity.description,
                TaskStatus.valueOf(entity.status),
                entity.previousTaskId,
                entity.knowledgeEntryId,
                entity.reflection,
                entity.isFlexible,
                TaskPriority.valueOf(entity.priority),
                entity.deadline != null ? LocalDate.parse(entity.deadline) : null,
                entity.estimatedDurationMinutes,
                entity.scheduledDate != null ? LocalDate.parse(entity.scheduledDate) : null,
                entity.scheduledTime != null ? LocalTime.parse(entity.scheduledTime) : null,
                entity.autoRescheduleCount,
                entity.completedAt != null ? Instant.ofEpochMilli(entity.completedAt) : null,
                entity.cancelledAt != null ? Instant.ofEpochMilli(entity.cancelledAt) : null,
                Instant.ofEpochMilli(entity.createdAt),
                Instant.ofEpochMilli(entity.updatedAt),
                tags
        );
    }

    public static TaskOccurrenceEntity toEntity(TaskOccurrence occurrence) {
        if (occurrence == null) {
            return null;
        }
        TaskOccurrenceEntity entity = new TaskOccurrenceEntity();
        entity.id = occurrence.id();
        entity.taskId = occurrence.taskId();
        entity.ruleId = occurrence.ruleId();
        entity.occurrenceDate = occurrence.occurrenceDate().toString();
        entity.effectiveDate = occurrence.effectiveDate().toString();
        entity.status = occurrence.status().name();
        entity.titleOverride = occurrence.titleOverride();
        entity.timeOverride = occurrence.timeOverride() != null ? occurrence.timeOverride().toString() : null;
        entity.noteOverride = occurrence.noteOverride();
        entity.reflection = occurrence.reflection();
        entity.autoRescheduleCount = occurrence.autoRescheduleCount();
        entity.completedAt = occurrence.completedAt() != null ? occurrence.completedAt().toEpochMilli() : null;
        entity.cancelledAt = occurrence.cancelledAt() != null ? occurrence.cancelledAt().toEpochMilli() : null;
        entity.createdAt = occurrence.createdAt().toEpochMilli();
        entity.updatedAt = occurrence.updatedAt().toEpochMilli();
        return entity;
    }

    public static TaskOccurrence toDomain(TaskOccurrenceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TaskOccurrence(
                entity.id,
                entity.taskId,
                entity.ruleId,
                LocalDate.parse(entity.occurrenceDate),
                LocalDate.parse(entity.effectiveDate),
                OccurrenceStatus.valueOf(entity.status),
                entity.titleOverride,
                entity.timeOverride != null ? LocalTime.parse(entity.timeOverride) : null,
                entity.noteOverride,
                entity.reflection,
                entity.autoRescheduleCount,
                entity.completedAt != null ? Instant.ofEpochMilli(entity.completedAt) : null,
                entity.cancelledAt != null ? Instant.ofEpochMilli(entity.cancelledAt) : null,
                Instant.ofEpochMilli(entity.createdAt),
                Instant.ofEpochMilli(entity.updatedAt)
        );
    }

    public static RecurrenceRuleEntity toEntity(RecurrenceRule rule) {
        if (rule == null) {
            return null;
        }
        RecurrenceRuleEntity entity = new RecurrenceRuleEntity();
        entity.id = rule.id();
        entity.taskId = rule.taskId();
        entity.type = rule.type().name();
        entity.interval = rule.interval();
        entity.weekdays = rule.weekdays();
        entity.startDate = rule.startDate().toString();
        entity.endDate = rule.endDate() != null ? rule.endDate().toString() : null;
        entity.occurrenceCount = rule.occurrenceCount();
        entity.anchorDayOfMonth = rule.anchorDayOfMonth();
        entity.missedPolicy = rule.missedPolicy().name();
        entity.createdAt = rule.createdAt().toEpochMilli();
        entity.updatedAt = rule.updatedAt().toEpochMilli();
        return entity;
    }

    public static RecurrenceRule toDomain(RecurrenceRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RecurrenceRule(
                entity.id,
                entity.taskId,
                RecurrenceType.valueOf(entity.type),
                entity.interval,
                entity.weekdays,
                LocalDate.parse(entity.startDate),
                entity.endDate != null ? LocalDate.parse(entity.endDate) : null,
                entity.occurrenceCount,
                entity.anchorDayOfMonth,
                MissedPolicy.valueOf(entity.missedPolicy),
                Instant.ofEpochMilli(entity.createdAt),
                Instant.ofEpochMilli(entity.updatedAt)
        );
    }

    public static TaskEventEntity toEntity(TaskEvent event) {
        if (event == null) {
            return null;
        }
        TaskEventEntity entity = new TaskEventEntity();
        entity.id = event.id();
        entity.taskId = event.taskId();
        entity.occurrenceId = event.occurrenceId();
        entity.eventType = event.eventType();
        entity.details = event.details();
        entity.timestamp = event.timestamp().toEpochMilli();
        return entity;
    }

    public static TaskEvent toDomain(TaskEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TaskEvent(
                entity.id,
                entity.taskId,
                entity.occurrenceId,
                entity.eventType,
                entity.details,
                Instant.ofEpochMilli(entity.timestamp)
        );
    }

    public static RescheduleHistoryEntity toEntity(RescheduleRecord record) {
        if (record == null) {
            return null;
        }
        RescheduleHistoryEntity entity = new RescheduleHistoryEntity();
        entity.id = record.id();
        entity.taskId = record.taskId();
        entity.occurrenceId = record.occurrenceId();
        entity.fromDate = record.fromDate() != null ? record.fromDate().toString() : null;
        entity.toDate = record.toDate().toString();
        entity.reason = record.reason();
        entity.timestamp = record.timestamp().toEpochMilli();
        return entity;
    }

    public static RescheduleRecord toDomain(RescheduleHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RescheduleRecord(
                entity.id,
                entity.taskId,
                entity.occurrenceId,
                entity.fromDate != null ? LocalDate.parse(entity.fromDate) : null,
                LocalDate.parse(entity.toDate),
                entity.reason,
                Instant.ofEpochMilli(entity.timestamp)
        );
    }

    public static ReminderStateEntity toEntity(ReminderState reminder) {
        if (reminder == null) {
            return null;
        }
        ReminderStateEntity entity = new ReminderStateEntity();
        entity.id = reminder.id();
        entity.reminderType = reminder.reminderType();
        entity.ownerType = reminder.ownerType();
        entity.ownerId = reminder.ownerId();
        entity.triggerAt = reminder.triggerAt().toEpochMilli();
        entity.triggerLocalDate = reminder.triggerLocalDate().toString();
        entity.triggerLocalTime = reminder.triggerLocalTime() != null ? reminder.triggerLocalTime().toString() : null;
        entity.channel = reminder.channel();
        entity.status = reminder.status();
        entity.firedAt = reminder.firedAt() != null ? reminder.firedAt().toEpochMilli() : null;
        entity.idempotencyKey = reminder.idempotencyKey();
        entity.createdAt = reminder.createdAt().toEpochMilli();
        return entity;
    }

    public static ReminderState toDomain(ReminderStateEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ReminderState(
                entity.id,
                entity.reminderType,
                entity.ownerType,
                entity.ownerId,
                Instant.ofEpochMilli(entity.triggerAt),
                LocalDate.parse(entity.triggerLocalDate),
                entity.triggerLocalTime != null ? LocalTime.parse(entity.triggerLocalTime) : null,
                entity.channel,
                entity.status,
                entity.firedAt != null ? Instant.ofEpochMilli(entity.firedAt) : null,
                entity.idempotencyKey,
                Instant.ofEpochMilli(entity.createdAt)
        );
    }
}
