import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { GymDashboard, Member, MemberDetail, MemberRequest, Membership, MembershipRequest, MembershipStatus, Plan, PlanRequest } from '@/app/core/models/api.models';

/** Endpoints del gimnasio del usuario (ADMIN / STAFF). */
@Injectable({ providedIn: 'root' })
export class GymApiService {
    private readonly http = inject(HttpClient);
    private readonly api = `${environment.apiUrl}/api`;

    dashboard(): Observable<GymDashboard> {
        return this.http.get<GymDashboard>(`${this.api}/gym/dashboard`);
    }

    // Planes
    listPlans(): Observable<Plan[]> {
        return this.http.get<Plan[]>(`${this.api}/plans`);
    }

    createPlan(body: PlanRequest): Observable<Plan> {
        return this.http.post<Plan>(`${this.api}/plans`, body);
    }

    updatePlan(id: number, body: PlanRequest): Observable<Plan> {
        return this.http.put<Plan>(`${this.api}/plans/${id}`, body);
    }

    setPlanActive(id: number, active: boolean): Observable<Plan> {
        return this.http.patch<Plan>(`${this.api}/plans/${id}/status`, { active });
    }

    deletePlan(id: number): Observable<void> {
        return this.http.delete<void>(`${this.api}/plans/${id}`);
    }

    // Socios
    listMembers(filters: { q?: string; status?: MembershipStatus | null } = {}): Observable<Member[]> {
        let params = new HttpParams();
        if (filters.q?.trim()) params = params.set('q', filters.q.trim());
        if (filters.status) params = params.set('status', filters.status);
        return this.http.get<Member[]>(`${this.api}/members`, { params });
    }

    getMember(id: number): Observable<MemberDetail> {
        return this.http.get<MemberDetail>(`${this.api}/members/${id}`);
    }

    createMember(body: MemberRequest): Observable<Member> {
        return this.http.post<Member>(`${this.api}/members`, body);
    }

    updateMember(id: number, body: MemberRequest): Observable<Member> {
        return this.http.put<Member>(`${this.api}/members/${id}`, body);
    }

    setMemberActive(id: number, active: boolean): Observable<Member> {
        return this.http.patch<Member>(`${this.api}/members/${id}/status`, { active });
    }

    deleteMember(id: number): Observable<void> {
        return this.http.delete<void>(`${this.api}/members/${id}`);
    }

    // Membresías
    sellMembership(memberId: number, body: MembershipRequest): Observable<Membership> {
        return this.http.post<Membership>(`${this.api}/members/${memberId}/memberships`, body);
    }

    cancelMembership(id: number): Observable<Membership> {
        return this.http.patch<Membership>(`${this.api}/memberships/${id}/cancel`, {});
    }
}
