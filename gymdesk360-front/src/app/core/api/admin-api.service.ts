import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { AdminUserRequest, ApiError, CreateGymRequest, Gym, Stats, UpdateGymRequest, User } from '@/app/core/models/api.models';

/** Endpoints del panel SUPER_ADMIN. */
@Injectable({ providedIn: 'root' })
export class AdminApiService {
    private readonly http = inject(HttpClient);
    private readonly api = `${environment.apiUrl}/api`;

    // Métricas
    stats(): Observable<Stats> {
        return this.http.get<Stats>(`${this.api}/admin/stats`);
    }

    // Gimnasios
    listGyms(): Observable<Gym[]> {
        return this.http.get<Gym[]>(`${this.api}/gyms`);
    }

    createGym(body: CreateGymRequest): Observable<Gym> {
        return this.http.post<Gym>(`${this.api}/gyms`, body);
    }

    updateGym(id: number, body: UpdateGymRequest): Observable<Gym> {
        return this.http.put<Gym>(`${this.api}/gyms/${id}`, body);
    }

    setGymActive(id: number, active: boolean): Observable<Gym> {
        return this.http.patch<Gym>(`${this.api}/gyms/${id}/status`, { active });
    }

    deleteGym(id: number): Observable<void> {
        return this.http.delete<void>(`${this.api}/gyms/${id}`);
    }

    // Usuarios
    listUsers(gymId?: number | null): Observable<User[]> {
        const params = gymId ? new HttpParams().set('gymId', gymId) : undefined;
        return this.http.get<User[]>(`${this.api}/admin/users`, { params });
    }

    createUser(body: AdminUserRequest): Observable<User> {
        return this.http.post<User>(`${this.api}/admin/users`, body);
    }

    updateUser(id: number, body: AdminUserRequest): Observable<User> {
        return this.http.put<User>(`${this.api}/admin/users/${id}`, body);
    }

    setUserEnabled(id: number, active: boolean): Observable<User> {
        return this.http.patch<User>(`${this.api}/admin/users/${id}/status`, { active });
    }

    resetPassword(id: number, password: string): Observable<void> {
        return this.http.put<void>(`${this.api}/admin/users/${id}/password`, { password });
    }

    deleteUser(id: number): Observable<void> {
        return this.http.delete<void>(`${this.api}/admin/users/${id}`);
    }
}

/** Mensaje legible a partir de un error de la API. */
export function apiErrorMessage(err: unknown, fallback = 'Ocurrió un error inesperado'): string {
    if (err instanceof HttpErrorResponse) {
        if (err.status === 0) {
            return 'No se pudo conectar con el servidor';
        }
        const body = err.error as Partial<ApiError> | null;
        if (body?.fieldErrors && Object.keys(body.fieldErrors).length) {
            return Object.entries(body.fieldErrors)
                .map(([field, msg]) => `${FIELD_LABELS[field] ?? field}: ${msg}`)
                .join(' · ');
        }
        if (body?.message) {
            return body.message;
        }
    }
    return fallback;
}

/** Nombres legibles de los campos que valida el backend. */
const FIELD_LABELS: Record<string, string> = {
    name: 'Nombre',
    slug: 'Slug',
    email: 'Email',
    password: 'Contraseña',
    fullName: 'Nombre completo',
    role: 'Rol',
    gymId: 'Gimnasio',
    adminEmail: 'Email del administrador',
    adminPassword: 'Contraseña del administrador',
    adminFullName: 'Nombre del administrador',
    currentPassword: 'Contraseña actual',
    newPassword: 'Nueva contraseña',
    price: 'Precio',
    durationValue: 'Duración',
    durationUnit: 'Unidad de duración',
    description: 'Descripción',
    rut: 'RUT',
    firstName: 'Nombres',
    lastName: 'Apellidos',
    phone: 'Teléfono',
    birthDate: 'Fecha de nacimiento',
    emergencyContactName: 'Contacto de emergencia',
    emergencyContactPhone: 'Teléfono de emergencia',
    notes: 'Notas',
    planId: 'Plan',
    startDate: 'Fecha de inicio',
    pricePaid: 'Precio cobrado'
};
