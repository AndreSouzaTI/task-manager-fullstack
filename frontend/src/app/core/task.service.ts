import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page, Task, TaskPriority, TaskRequest, TaskStatus, TaskSummary } from './models';

export interface TaskFilter {
  search?: string;
  priority?: TaskPriority | '';
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tasks';

  list(filter: TaskFilter = {}): Observable<Page<Task>> {
    let params = new HttpParams().set('size', 200);
    if (filter.search) {
      params = params.set('search', filter.search);
    }
    if (filter.priority) {
      params = params.set('priority', filter.priority);
    }
    return this.http.get<Page<Task>>(this.baseUrl, { params });
  }

  summary(): Observable<TaskSummary> {
    return this.http.get<TaskSummary>(`${this.baseUrl}/summary`);
  }

  create(task: TaskRequest): Observable<Task> {
    return this.http.post<Task>(this.baseUrl, task);
  }

  update(id: number, task: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/${id}`, task);
  }

  changeStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${this.baseUrl}/${id}/status`, { status });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
