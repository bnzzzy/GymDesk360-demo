import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { GymApiService } from '@/app/core/api/gym-api.service';
import { AuthService } from '@/app/core/auth/auth.service';
import { GymDashboard } from '@/app/core/models/api.models';
import { initials } from '@/app/shared/ui-helpers';

@Component({
    selector: 'app-gym-dashboard',
    standalone: true,
    imports: [RouterModule, CurrencyPipe, DatePipe, AvatarModule, ButtonModule, SkeletonModule],
    templateUrl: './gym-dashboard.html',
    styleUrl: './gym-dashboard.scss'
})
export class GymDashboardPage implements OnInit {
    private readonly api = inject(GymApiService);
    private readonly auth = inject(AuthService);
    private readonly toast = inject(MessageService);

    readonly data = signal<GymDashboard | null>(null);
    readonly loading = signal(true);

    readonly firstName = computed(() => this.auth.user()?.fullName.split(' ')[0] ?? '');
    readonly isAdmin = computed(() => this.auth.hasRole('ADMIN'));
    readonly month = new Date();

    readonly cards = computed(() => {
        const d = this.data();
        return [
            { label: 'Socios activos', value: String(d?.activeMembers ?? 0), hint: `de ${d?.totalMembers ?? 0} registrados`, icon: 'pi pi-users', link: '/socios', query: { estado: 'ACTIVE' } },
            { label: 'Vencidos (30 días)', value: String(d?.expiredLast30Days ?? 0), hint: 'por recuperar', icon: 'pi pi-user-minus', link: '/socios', query: { estado: 'EXPIRED' } },
            { label: 'Ventas del mes', value: String(d?.salesThisMonth ?? 0), hint: (d?.newMembersThisMonth ?? 0) === 1 ? '1 socio nuevo' : `${d?.newMembersThisMonth ?? 0} socios nuevos`, icon: 'pi pi-shopping-cart', link: null, query: null },
            { label: 'Ingresos del mes', value: null, money: d?.revenueThisMonth ?? 0, hint: 'membresías vendidas', icon: 'pi pi-wallet', link: null, query: null }
        ];
    });

    ngOnInit() {
        this.api.dashboard().subscribe({
            next: (d) => {
                this.data.set(d);
                this.loading.set(false);
            },
            error: (err) => {
                this.loading.set(false);
                this.toast.add({ severity: 'error', summary: 'Error', detail: apiErrorMessage(err), life: 5000 });
            }
        });
    }

    initials = initials;

    daysLabel(days: number | null): string {
        if (days === 0) return 'Vence hoy';
        if (days === 1) return 'Vence mañana';
        return `Vence en ${days} días`;
    }
}
