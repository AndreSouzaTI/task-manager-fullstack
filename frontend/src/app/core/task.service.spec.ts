import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TaskService } from './task.service';

describe('TaskService', () => {
  let service: TaskService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the filters that were informed', () => {
    service.list({ search: 'relatório', priority: '' }).subscribe();

    const request = http.expectOne((r) => r.url === '/api/tasks');
    expect(request.request.params.get('search')).toBe('relatório');
    expect(request.request.params.has('priority')).toBe(false);
    request.flush({ content: [], page: 0, size: 200, totalElements: 0, totalPages: 0 });
  });

  it('moves a task with PATCH on the status endpoint', () => {
    service.changeStatus(7, 'DONE').subscribe();

    const request = http.expectOne('/api/tasks/7/status');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'DONE' });
    request.flush({});
  });
});
