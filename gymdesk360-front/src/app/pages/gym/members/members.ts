import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { GymApiService } from '@/app/core/api/gym-api.service';
import { Member, MembershipStatus, Plan } from '@/app/core/models/api.models';
import { MemberFormDialog } from '@/app/pages/gym/components/member-form-dialog/member-form-dialog';
import { SellMembershipDialog } from '@/app/pages/gym/components/sell-membership-dialog/sell-membership-dialog';
import { initials, MEMBER_STATUS_OPTIONS, STATUS_LABELS, STATUS_SEVERITY } from '@/app/shared/ui-helpers';
import { formatRut } from '@/app/shared/utils/rut';

@Component({
    selector: 'app-members',
    standalone: true,
    imports: [FormsModule, RouterModule, DatePipe, AvatarModule, ButtonModule, IconFieldModule, InputIconModule, InputTextModule, TableModule, TagModule, TooltipModule, MemberFormDialog, SellMembershipDialog],
    templateUrl: './members.html',
    styleUrl: './members.scss'
})
export class MembersPage implements OnInit {
    private readonly api = inject(GymApiService);
    private readonly toast = inject(MessageService);
    private readonly router = inject(Router);

    /** ?estado=EXPIRED preselecciona el filtro (atajo desde el panel). */
    readonly estado = input<string>();
    /** ?nuevo=1 abre el formulario de alta (atajo desde el panel). */
    readonly nuevo = input<string>();

    readonly members = signal<Member[]>([]);
    readonly plans = signal<Plan[]>([]);
    readonly loading = signal(true);
    readonly search = signal('');
    readonly statusFilter = signal<MembershipStatus | null>(null);
    readonly showInactive = signal(false);

    readonly statusOptions = MEMBER_STATUS_OPTIONS;

    /** Cantidad de socios por estado (sobre los socios activos en la ficha). */
    readonly counts = computed(() => {
        const c: Partial<Record<MembershipStatus, number>> = {};
        for (const m of this.members()) {
            if (!m.active) continue;
            c[m.status] = (c[m.status] ?? 0) + 1;
        }
        return c;
    });

    readonly inactiveCount = computed(() => this.members().filter((m) => !m.active).length);

    readonly filtered = computed(() => {
        const needle = fold(this.search());
        const status = this.statusFilter();
        const showInactive = this.showInactive();
        return this.members().filter(
            (m) =>
                (showInactive ? !m.active : m.active) &&
                (!status || m.status === status) &&
                (!needle || fold([m.fullName, m.rut ?? '', (m.rut ?? '').replace('-', ''), m.email ?? '', m.phone ?? ''].join(' ')).includes(needle))
        );
    });

    // Diálogos
    formVisible = false;
    sellVisible = false;
    readonly editing = signal<Member | null>(null);
    readonly selling = signal<Member | null>(null);

    ngOnInit() {
        const e = this.estado() as MembershipStatus | undefined;
        if (e && MEMBER_STATUS_OPTIONS.some((o) => o.value === e)) {
            this.statusFilter.set(e);
        }
        this.load();
        this.api.listPlans().subscribe({ next: (p) => this.plans.set(p) });
        if (this.nuevo()) {
            this.openNew();
            this.router.navigate([], { queryParams: {}, replaceUrl: true });
        }
    }

    load() {
        this.loading.set(true);
        this.api.listMembers().subscribe({
            next: (members) => {
                this.members.set(members);
                this.loading.set(false);
            },
            error: (err) => {
                this.loading.set(false);
                this.toast.add({ severity: 'error', summary: 'Error', detail: apiErrorMessage(err), life: 5000 });
            }
        });
    }

    setStatus(s: MembershipStatus | null) {
        this.showInactive.set(false);
        this.statusFilter.set(this.statusFilter() === s ? null : s);
    }

    toggleInactive() {
        this.statusFilter.set(null);
        this.showInactive.update((v) => !v);
    }

    openNew() {
        this.editing.set(null);
        this.formVisible = true;
    }

    openEdit(m: Member, event?: Event) {
        event?.stopPropagation();
        this.editing.set(m);
        this.formVisible = true;
    }

    openSell(m: Member, event?: Event) {
        event?.stopPropagation();
        this.selling.set(m);
        this.sellVisible = true;
    }

    open(m: Member) {
        this.router.navigate(['/socios', m.id]);
    }

    onSaved(m: Member) {
        const exists = this.members().some((x) => x.id === m.id);
        if (exists) {
            this.members.update((list) => list.map((x) => (x.id === m.id ? m : x)));
        } else {
            // Recién creado: ir a su ficha para venderle un plan
            this.router.navigate(['/socios', m.id], { queryParams: { vender: 1 } });
        }
    }

    // Helpers de plantilla
    rut = formatRut;
    initials = initials;
    statusLabel = (s: MembershipStatus) => STATUS_LABELS[s];
    statusSeverity = (s: MembershipStatus) => STATUS_SEVERITY[s];
}

function fold(s: string): string {
    return s
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .trim();
}
