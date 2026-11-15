import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, map, Observable, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '@/environments/environment';
import { AuthResponse, Role, User } from '@/app/core/models/api.models';

const STORAGE_KEY = 'gd360.session';

interface StoredSession {
    accessToken: string;
    refreshToken: string;
    user: User;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly api = `${environment.apiUrl}/api/auth`;

    private readonly session = signal<StoredSession | null>(readSession());
    /** Refresh en curso: si varias peticiones reciben 401 a la vez, todas esperan el mismo. */
    private refreshInFlight$: Observable<string> | null = null;

    readonly user = computed(() => this.session()?.user ?? null);
    readonly isLoggedIn = computed(() => this.session() !== null);
    readonly isSuperAdmin = computed(() => this.user()?.role === 'SUPER_ADMIN');

    get accessToken(): string | null {
        return this.session()?.accessToken ?? null;
    }

    login(email: string, password: string): Observable<User> {
        return this.http.post<AuthResponse>(`${this.api}/login`, { email, password }).pipe(
            tap((res) => this.store(res)),
            map((res) => res.user)
        );
    }

    /** Demo: entra con una cuenta de demostración sin contraseña. */
    demoLogin(account: 'admin' | 'staff' | 'super-admin'): Observable<User> {
        return this.http.post<AuthResponse>(`${this.api}/demo/${account}`, {}).pipe(
            tap((res) => this.store(res)),
            map((res) => res.user)
        );
    }

    /** Cambia la contraseña propia; el servidor cierra las otras sesiones y entrega tokens nuevos. */
    changePassword(currentPassword: string, newPassword: string): Observable<void> {
        return this.http.put<AuthResponse>(`${this.api}/password`, { currentPassword, newPassword }).pipe(
            tap((res) => this.store(res)),
            map(() => undefined)
        );
    }

    refreshAccessToken(): Observable<string> {
        const refreshToken = this.session()?.refreshToken;
        if (!refreshToken) {
            return throwError(() => new Error('Sin refresh token'));
        }
        if (!this.refreshInFlight$) {
            this.refreshInFlight$ = this.http.post<AuthResponse>(`${this.api}/refresh`, { refreshToken }).pipe(
                tap((res) => this.store(res)),
                map((res) => res.accessToken),
                finalize(() => (this.refreshInFlight$ = null)),
                shareReplay(1)
            );
        }
        return this.refreshInFlight$;
    }

    /** Revoca el refresh token en el servidor (sin esperar) y limpia la sesión local. */
    logout(redirect = true): void {
        const refreshToken = this.session()?.refreshToken;
        if (refreshToken) {
            this.http.post(`${this.api}/logout`, { refreshToken }).subscribe({ error: () => {} });
        }
        this.clear();
        if (redirect) {
            this.router.navigate(['/auth/login']);
        }
    }

    /** Ruta de inicio según el rol. */
    homeFor(role: Role | undefined): string {
        switch (role) {
            case 'SUPER_ADMIN':
                return '/admin';
            case 'ADMIN':
            case 'STAFF':
                return '/panel';
            default:
                return '/inicio';
        }
    }

    hasRole(...roles: Role[]): boolean {
        const role = this.user()?.role;
        return !!role && roles.includes(role);
    }

    clear(): void {
        this.session.set(null);
        try {
            localStorage.removeItem(STORAGE_KEY);
        } catch {
            /* almacenamiento no disponible */
        }
    }

    private store(res: AuthResponse): void {
        const s: StoredSession = { accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user };
        this.session.set(s);
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
        } catch {
            /* almacenamiento no disponible: la sesión vive solo en memoria */
        }
    }
}

function readSession(): StoredSession | null {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? (JSON.parse(raw) as StoredSession) : null;
    } catch {
        return null;
    }
}
