package com.andresouza.tasks.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record TaskRequest(
            @NotBlank @Size(max = 140) String title,
            @Size(max = 2000) String description,
            @NotNull TaskPriority priority,
            LocalDate dueDate) {
    }

    public record StatusRequest(@NotNull TaskStatus status) {
    }

    public record TaskResponse(Long id, String title, String description, TaskStatus status, TaskPriority priority,
                               LocalDate dueDate, boolean overdue, Instant completedAt, Instant createdAt) {

        static TaskResponse from(Task task, LocalDate today) {
            return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                    task.getPriority(), task.getDueDate(), task.isOverdue(today), task.getCompletedAt(), task.getCreatedAt());
        }
    }

    public record SummaryResponse(long todo, long inProgress, long done, long overdue) {

        public long total() {
            return todo + inProgress + done;
        }
    }
}
