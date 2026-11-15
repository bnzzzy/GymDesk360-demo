import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '@/app/core/auth/auth.service';

/** Endpoints que no llevan token ni disparan el refresh. */
const PUBLIC_ENDPOINTS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout', '/api/auth/demo/', '/api/public/'];

/**
 * 1. Agrega "Authorization: Bearer <access token>" a las llamadas a la API.
 * 2. Si la API responde 401, renueva el access token con el refresh token y repite la petición una vez.
 * 3. Si el refresh también falla, cierra la sesión y manda al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const auth = inject(AuthService);

    if (!req.url.includes('/api/') || PUBLIC_ENDPOINTS.some((p) => req.url.includes(p))) {
        return next(req);
    }

    return next(withToken(req, auth.accessToken)).pipe(
        catchError((err: unknown) => {
            if (!(err instanceof HttpErrorResponse) || err.status !== 401 || !auth.isLoggedIn()) {
                return throwError(() => err);
            }
            return auth.refreshAccessToken().pipe(
                catchError((refreshErr) => {
                    auth.logout();
                    return throwError(() => refreshErr);
                }),
                switchMap((token) => next(withToken(req, token)))
            );
        })
    );
};

function withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
    return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}
