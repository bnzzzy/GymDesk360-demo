import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { forkJoin } from 'rxjs';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { GymApiService } from '@/app/core/api/gym-api.service';
import { AuthService } from '@/app/core/auth/auth.service';
import { MemberDetail, Membership, MembershipStatus, Plan } from '@/app/core/models/api.models';
import { MemberFormDialog } from '@/app/pages/gym/components/member-form-dialog/member-form-dialog';
import { SellMembershipDialog } from '@/app/pages/gym/components/sell-membership-dialog/sell-membership-dialog';
import { initials, STATUS_LABELS, STATUS_SEVERITY } from '@/app/shared/ui-helpers';
import { fromIsoDate, today } from '@/app/shared/utils/dates';
import { formatRut } from '@/app/shared/utils/rut';

@Component({
    selector: 'app-member-detail',
    standalone: true,
    imports: [RouterModule, DatePipe, CurrencyPipe, AvatarModule, ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule, MemberFormDialog, SellMembershipDialog],
    templateUrl: './member-detail.html',
    styleUrl: './member-detail.scss'
})
export class MemberDetailPage implements OnInit {
    private readonly api = inject(GymApiService);
    private readonly auth = inject(AuthService);
    private readonly toast = inject(MessageService);
    private readonly confirm = inject(ConfirmationService);
    private readonly router = inject(Router);

    /** :id de la ruta. */
    readonly id = input.required<string>();
    /** ?vender=1 abre directamente la venta (al crear un socio nuevo). */
    readonly vender = input<string>();

    readonly detail = signal<MemberDetail | null>(null);
    readonly plans = signal<Plan[]>([]);
    readonly loading = signal(true);

    readonly isAdmin = computed(() => this.auth.hasRole('ADMIN'));
    readonly member = computed(() => this.detail()?.member ?? null);
    readonly memberships = computed(() => this.detail()?.memberships ?? []);

    /** Progreso de la membresía vigente (0-100). */
    readonly progress = computed(() => {
        const m = this.member();
        if (!m || m.status !== 'ACTIVE') return null;
        const start = fromIsoDate(m.currentStartDate)!.getTime();
        const end = fromIsoDate(m.currentEndDate)!.getTime();
        const total = end - start + 86_400_000;
        const elapsed = today().getTime() - start + 86_400_000;
        return Math.min(100, Math.max(0, Math.round((elapsed / total) * 100)));
    });

    readonly age = computed(() => {
        const b = fromIsoDate(this.member()?.birthDate);
        if (!b) return null;
        const t = today();
        let a = t.getFullYear() - b.getFullYear();
        if (t.getMonth() < b.getMonth() || (t.getMonth() === b.getMonth() && t.getDate() < b.getDate())) a--;
        return a;
    });

    readonly totalPaid = computed(() => this.memberships().filter((m) => m.status !== 'CANCELLED').reduce((sum, m) => sum + m.pricePaid, 0));

    formVisible = false;
    sellVisible = false;

    ngOnInit() {
        forkJoin({ detail: this.api.getMember(Number(this.id())), plans: this.api.listPlans() }).subscribe({
            next: ({ detail, plans }) => {
                this.detail.set(detail);
                this.plans.set(plans);
                this.loading.set(false);
                if (this.vender() && detail.member.active) {
                    this.sellVisible = true;
                    this.router.navigate([], { queryParams: {}, replaceUrl: true });
                }
            },
            error: (err) => {
                this.loading.set(false);
                this.fail(err);
                this.router.navigate(['/socios']);
            }
        });
    }

    reload() {
        this.api.getMember(Number(this.id())).subscribe({ next: (d) => this.detail.set(d), error: (err) => this.fail(err) });
    }

    toggleActive() {
        const m = this.member();
        if (!m) return;
        const activate = !m.active;
        this.confirm.confirm({
            header: activate ? 'Reactivar socio' : 'Desactivar socio',
            message: activate
                ? `¿Reactivar a ${m.fullName}?`
                : `¿Desactivar a ${m.fullName}? Dejará de aparecer en la lista principal; su historial se conserva.`,
            icon: activate ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
            acceptLabel: activate ? 'Reactivar' : 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: activate ? 'success' : 'warn' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.setMemberActive(m.id, activate).subscribe({
                    next: () => {
                        this.toast.add({ severity: 'success', summary: activate ? 'Socio reactivado' : 'Socio desactivado', detail: m.fullName });
                        this.reload();
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    confirmDelete() {
        const m = this.member();
        if (!m) return;
        this.confirm.confirm({
            header: 'Eliminar socio',
            message: this.memberships().length
                ? `${m.fullName} tiene membresías registradas, así que no se puede eliminar. Puedes desactivarlo.`
                : `¿Eliminar a ${m.fullName}? Esta acción no se puede deshacer.`,
            icon: 'pi pi-trash',
            acceptLabel: 'Eliminar',
            rejectLabel: 'Cancelar',
            acceptVisible: this.memberships().length === 0,
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.deleteMember(m.id).subscribe({
                    next: () => {
                        this.toast.add({ severity: 'success', summary: 'Socio eliminado', detail: m.fullName });
                        this.router.navigate(['/socios']);
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    confirmCancel(ms: Membership) {
        this.confirm.confirm({
            header: 'Anular membresía',
            message: `¿Anular "${ms.planName}" (${this.fmt(ms.startDate)} al ${this.fmt(ms.endDate)})? Quedará en el historial como anulada y dejará de contar como vigente y en las ventas del mes.`,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Anular',
            rejectLabel: 'Volver',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.cancelMembership(ms.id).subscribe({
                    next: () => {
                        this.toast.add({ severity: 'success', summary: 'Membresía anulada', detail: ms.planName });
                        this.reload();
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    // Helpers de plantilla
    rut = formatRut;
    initials = initials;
    statusLabel = (s: MembershipStatus) => STATUS_LABELS[s];
    statusSeverity = (s: MembershipStatus) => STATUS_SEVERITY[s];

    private fmt(iso: string) {
        const [y, m, d] = iso.split('-');
        return `${d}/${m}/${y}`;
    }

    private fail(err: unknown) {
        this.toast.add({ severity: 'error', summary: 'Error', detail: apiErrorMessage(err), life: 5000 });
    }
}
