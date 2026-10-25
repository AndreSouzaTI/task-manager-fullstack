import { Component, ElementRef, afterNextRender, effect, inject, input, output, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PRIORITY_LABELS, Task, TaskPriority, TaskRequest } from '../../core/models';

/** Formulário de criação e edição de tarefa, exibido como modal. */
@Component({
  selector: 'app-task-form',
  imports: [ReactiveFormsModule],
  templateUrl: './task-form.html',
  styleUrl: './task-form.scss',
})
export class TaskForm {
  readonly task = input<Task | null>(null);
  readonly saving = input(false);
  readonly save = output<TaskRequest>();
  readonly cancel = output<void>();

  protected readonly priorities = Object.entries(PRIORITY_LABELS) as [TaskPriority, string][];

  protected readonly form = inject(NonNullableFormBuilder).group({
    title: ['', [Validators.required, Validators.maxLength(140)]],
    description: ['', Validators.maxLength(2000)],
    priority: ['MEDIUM' as TaskPriority, Validators.required],
    dueDate: [''],
  });

  private readonly titleInput = viewChild<ElementRef<HTMLInputElement>>('titleInput');

  constructor() {
    afterNextRender(() => this.titleInput()?.nativeElement.focus());

    effect(() => {
      const task = this.task();
      this.form.reset({
        title: task?.title ?? '',
        description: task?.description ?? '',
        priority: task?.priority ?? 'MEDIUM',
        dueDate: task?.dueDate ?? '',
      });
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.save.emit({
      title: value.title.trim(),
      description: value.description.trim() || null,
      priority: value.priority,
      dueDate: value.dueDate || null,
    });
  }
}
