import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from './models';

/** Converte a resposta de erro da API (Problem Details) em uma mensagem amigável. */
export function errorMessage(error: unknown, fallback = 'Algo deu errado. Tente novamente.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor.';
    }
    const problem = error.error as ProblemDetail | null;
    const firstFieldError = problem?.errors ? Object.values(problem.errors)[0] : undefined;
    return firstFieldError ?? problem?.detail ?? fallback;
  }
  return fallback;
}
