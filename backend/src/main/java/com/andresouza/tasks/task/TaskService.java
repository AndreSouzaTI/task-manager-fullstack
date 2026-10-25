package com.andresouza.tasks.task;

import com.andresouza.tasks.exception.ResourceNotFoundException;
import com.andresouza.tasks.task.TaskDtos.SummaryResponse;
import com.andresouza.tasks.task.TaskDtos.TaskRequest;
import com.andresouza.tasks.task.TaskDtos.TaskResponse;
import com.andresouza.tasks.user.AppUser;
import com.andresouza.tasks.user.UserRepository;
import com.andresouza.tasks.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final UserRepository users;
    private final Clock clock;

    public TaskService(TaskRepository tasks, UserRepository users, Clock clock) {
        this.tasks = tasks;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> search(Long userId, TaskStatus status, TaskPriority priority, String search, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        return PageResponse.of(tasks.search(userId, status, priority, search, pageable), task -> TaskResponse.from(task, today));
    }

    @Transactional
    public TaskResponse create(Long userId, TaskRequest request) {
        AppUser owner = users.getReferenceById(userId);
        Task task = tasks.save(new Task(owner, request.title(), request.description(), request.priority(), request.dueDate()));
        return TaskResponse.from(task, LocalDate.now(clock));
    }

    @Transactional
    public TaskResponse update(Long userId, Long id, TaskRequest request) {
        Task task = getOwned(userId, id);
        task.update(request.title(), request.description(), request.priority(), request.dueDate());
        return TaskResponse.from(task, LocalDate.now(clock));
    }

    @Transactional
    public TaskResponse changeStatus(Long userId, Long id, TaskStatus status) {
        Task task = getOwned(userId, id);
        task.changeStatus(status);
        return TaskResponse.from(task, LocalDate.now(clock));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        tasks.delete(getOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(Long userId) {
        Map<TaskStatus, Long> counts = tasks.countByStatus(userId).stream()
                .collect(Collectors.toMap(TaskRepository.StatusCount::getStatus, TaskRepository.StatusCount::getTotal));
        return new SummaryResponse(
                counts.getOrDefault(TaskStatus.TODO, 0L),
                counts.getOrDefault(TaskStatus.IN_PROGRESS, 0L),
                counts.getOrDefault(TaskStatus.DONE, 0L),
                tasks.countOverdue(userId, LocalDate.now(clock)));
    }

    /** Tarefa de outro usuário responde 404, sem revelar que o id existe. */
    private Task getOwned(Long userId, Long id) {
        return tasks.findById(id)
                .filter(task -> task.belongsTo(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa", id));
    }
}
