import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', children: [] }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => backend.verify());

  function login(): void {
    auth.login('ana@email.com', 'senha-segura').subscribe();
    backend.expectOne('/api/auth/login').flush({ accessToken: 'jwt-123', expiresIn: 3600, name: 'Ana', email: 'ana@email.com' });
  }

  it('adds the bearer token to API calls after login', () => {
    login();

    http.get('/api/tasks').subscribe();

    expect(backend.expectOne('/api/tasks').request.headers.get('Authorization')).toBe('Bearer jwt-123');
  });

  it('does not send the token to authentication endpoints', () => {
    login();

    http.post('/api/auth/register', {}).subscribe();

    expect(backend.expectOne('/api/auth/register').request.headers.has('Authorization')).toBe(false);
  });

  it('ends the session when the API answers 401', () => {
    login();
    expect(auth.isAuthenticated()).toBe(true);

    http.get('/api/tasks').subscribe({ error: () => undefined });
    backend.expectOne('/api/tasks').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.isAuthenticated()).toBe(false);
    expect(localStorage.length).toBe(0);
  });
});
