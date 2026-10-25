import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/errors';
import {
  PRIORITY_LABELS,
  STATUS_LABELS,
  Task,
  TaskPriority,
  TaskRequest,
  TaskStatus,
  TaskSummary,
} from '../../core/models';
import { TaskService } from '../../core/task.service';
import { TaskForm } from './task-form';

const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

@Component({
  selector: 'app-board',
  imports: [DatePipe, TaskForm],
  templateUrl: './board.html',
  styleUrl: './board.scss',
})
export class Board {
  private readonly taskService = inject(TaskService);
  protected readonly auth = inject(AuthService);

  protected readonly statuses = STATUSES;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly priorityLabels = PRIORITY_LABELS;
  protected readonly priorities = Object.entries(PRIORITY_LABELS) as [TaskPriority, string][];

  protected readonly tasks = signal<Task[]>([]);
  protected readonly summary = signal<TaskSummary | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly priority = signal<TaskPriority | ''>('');

  protected readonly formOpen = signal(false);
  protected readonly editing = signal<Task | null>(null);
  protected readonly saving = signal(false);
  protected readonly dragging = signal<Task | null>(null);

  /** Tarefas agrupadas por coluna, recalculadas automaticamente quando a lista muda. */
  protected readonly columns = computed(() => {
    const grouped: Record<TaskStatus, Task[]> = { TODO: [], IN_PROGRESS: [], DONE: [] };
    for (const task of this.tasks()) {
      grouped[task.status].push(task);
    }
    return grouped;
  });

  private readonly searchChanges = new Subject<string>();

  constructor() {
    this.searchChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((term) => {
        this.search.set(term);
        this.load();
      });
    this.load();
  }

  protected onSearch(term: string): void {
    this.searchChanges.next(term.trim());
  }

  protected onPriorityFilter(priority: string): void {
    this.priority.set(priority as TaskPriority | '');
    this.load();
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.formOpen.set(true);
  }

  protected openEdit(task: Task): void {
    this.editing.set(task);
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editing.set(null);
  }

  protected save(request: TaskRequest): void {
    const editing = this.editing();
    const operation = editing ? this.taskService.update(editing.id, request) : this.taskService.create(request);
    this.saving.set(true);
    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  protected move(task: Task, status: TaskStatus): void {
    if (task.status === status) {
      return;
    }
    // Atualização otimista: a UI muda na hora e volta atrás se a API falhar
    const previous = this.tasks();
    this.tasks.update((list) => list.map((t) => (t.id === task.id ? { ...t, status } : t)));
    this.taskService.changeStatus(task.id, status).subscribe({
      next: (updated) => {
        this.tasks.update((list) => list.map((t) => (t.id === updated.id ? updated : t)));
        this.loadSummary();
      },
      error: (err) => {
        this.tasks.set(previous);
        this.error.set(errorMessage(err));
      },
    });
  }

  protected remove(task: Task): void {
    if (!confirm(`Excluir a tarefa "${task.title}"?`)) {
      return;
    }
    this.taskService.delete(task.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected neighbor(status: TaskStatus, direction: -1 | 1): TaskStatus | null {
    return STATUSES[STATUSES.indexOf(status) + direction] ?? null;
  }

  protected onDrop(status: TaskStatus, event: DragEvent): void {
    event.preventDefault();
    const task = this.dragging();
    this.dragging.set(null);
    if (task) {
      this.move(task, status);
    }
  }

  private load(): void {
    this.loading.set(true);
    this.taskService.list({ search: this.search(), priority: this.priority() }).subscribe({
      next: (page) => {
        this.tasks.set(page.content);
        this.loading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
    });
    this.loadSummary();
  }

  private loadSummary(): void {
    this.taskService.summary().subscribe((summary) => this.summary.set(summary));
  }
}
