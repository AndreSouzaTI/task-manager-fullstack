package com.andresouza.tasks.task;

import com.andresouza.tasks.task.TaskDtos.StatusRequest;
import com.andresouza.tasks.task.TaskDtos.SummaryResponse;
import com.andresouza.tasks.task.TaskDtos.TaskRequest;
import com.andresouza.tasks.task.TaskDtos.TaskResponse;
import com.andresouza.tasks.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tarefas")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Lista as tarefas do usuário com filtros e paginação")
    public PageResponse<TaskResponse> search(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) TaskStatus status,
                                             @RequestParam(required = false) TaskPriority priority,
                                             @RequestParam(defaultValue = "") String search,
                                             @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return taskService.search(userId(jwt), status, priority, search, pageable);
    }

    @GetMapping("/summary")
    @Operation(summary = "Totais por status e tarefas atrasadas")
    public SummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
        return taskService.summary(userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma tarefa")
    public TaskResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TaskRequest request) {
        return taskService.create(userId(jwt), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita uma tarefa")
    public TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(userId(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move a tarefa entre as colunas do quadro")
    public TaskResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return taskService.changeStatus(userId(jwt), id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma tarefa")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        taskService.delete(userId(jwt), id);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
