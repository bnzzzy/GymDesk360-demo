import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { AdminApiService, apiErrorMessage } from '@/app/core/api/admin-api.service';
import { AuthService } from '@/app/core/auth/auth.service';
import { Gym, Role, ROLE_LABELS, Stats } from '@/app/core/models/api.models';

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [RouterModule, DatePipe, ButtonModule, SkeletonModule, TagModule],
    templateUrl: './admin-dashboard.html',
    styleUrl: './admin-dashboard.scss'
})
export class AdminDashboard implements OnInit {
    private readonly api = inject(AdminApiService);
    private readonly toast = inject(MessageService);
    private readonly auth = inject(AuthService);

    readonly loading = signal(true);
    readonly stats = signal<Stats | null>(null);
    readonly gyms = signal<Gym[]>([]);

    readonly firstName = computed(() => this.auth.user()?.fullName.split(' ')[0] ?? '');

    readonly cards = computed(() => {
        const s = this.stats();
        return [
            { label: 'Gimnasios', value: s?.totalGyms ?? 0, icon: 'pi pi-building' },
            { label: 'Gimnasios activos', value: s?.activeGyms ?? 0, icon: 'pi pi-check-circle' },
            { label: 'Usuarios totales', value: s?.totalUsers ?? 0, icon: 'pi pi-users' },
            { label: 'Socios en la plataforma', value: s?.totalMembers ?? 0, icon: 'pi pi-id-card' }
        ];
    });

    readonly recentGyms = computed(() =>
        [...this.gyms()].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5)
    );

    readonly roleBars = computed(() => {
        const s = this.stats();
        const total = s?.totalUsers || 1;
        return (['ADMIN', 'STAFF', 'MEMBER', 'SUPER_ADMIN'] as Role[]).map((role) => {
            const count = s?.usersByRole[role] ?? 0;
            return { role, label: ROLE_LABELS[role], count, pct: Math.round((count / total) * 100) };
        });
    });

    ngOnInit() {
        forkJoin({ stats: this.api.stats(), gyms: this.api.listGyms() }).subscribe({
            next: ({ stats, gyms }) => {
                this.stats.set(stats);
                this.gyms.set(gyms);
                this.loading.set(false);
            },
            error: (err) => {
                this.loading.set(false);
                this.toast.add({ severity: 'error', summary: 'Error', detail: apiErrorMessage(err) });
            }
        });
    }
}
