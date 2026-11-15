import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { GymApiService } from '@/app/core/api/gym-api.service';
import { AuthService } from '@/app/core/auth/auth.service';
import { DurationUnit, Plan, PlanRequest } from '@/app/core/models/api.models';
import { FieldError } from '@/app/shared/components/field-error/field-error';
import { durationLabel } from '@/app/shared/ui-helpers';
import { validateForm } from '@/app/shared/utils/forms';

interface PlanForm {
    name: string;
    description: string;
    price: number | null;
    durationValue: number | null;
    durationUnit: DurationUnit;
}

/** Atajos para crear los planes más comunes con un clic. */
const PRESETS: { label: string; value: number; unit: DurationUnit }[] = [
    { label: 'Mensual', value: 1, unit: 'MONTHS' },
    { label: 'Trimestral', value: 3, unit: 'MONTHS' },
    { label: 'Semestral', value: 6, unit: 'MONTHS' },
    { label: 'Anual', value: 12, unit: 'MONTHS' },
    { label: 'Semanal', value: 7, unit: 'DAYS' },
    { label: 'Diario', value: 1, unit: 'DAYS' }
];

@Component({
    selector: 'app-plans',
    standalone: true,
    imports: [FormsModule, CurrencyPipe, ButtonModule, DialogModule, InputNumberModule, InputTextModule, SelectButtonModule, SkeletonModule, TagModule, TextareaModule, TooltipModule, FieldError],
    templateUrl: './plans.html',
    styleUrl: './plans.scss'
})
export class PlansPage implements OnInit {
    private readonly api = inject(GymApiService);
    private readonly auth = inject(AuthService);
    private readonly toast = inject(MessageService);
    private readonly confirm = inject(ConfirmationService);

    readonly plans = signal<Plan[]>([]);
    readonly loading = signal(true);
    readonly saving = signal(false);
    readonly editing = signal<Plan | null>(null);

    /** Solo ADMIN administra planes; STAFF los ve. */
    readonly canManage = computed(() => this.auth.hasRole('ADMIN'));
    readonly activeCount = computed(() => this.plans().filter((p) => p.active).length);

    readonly presets = PRESETS;
    readonly unitOptions = [
        { label: 'Meses', value: 'MONTHS' as DurationUnit },
        { label: 'Días', value: 'DAYS' as DurationUnit }
    ];

    dialogVisible = false;
    form: PlanForm = this.emptyForm();

    ngOnInit() {
        this.api.listPlans().subscribe({
            next: (plans) => {
                this.plans.set(plans);
                this.loading.set(false);
            },
            error: (err) => this.fail(err)
        });
    }

    duration(p: { durationValue: number; durationUnit: DurationUnit }) {
        return durationLabel(p.durationValue, p.durationUnit);
    }

    openNew() {
        this.editing.set(null);
        this.form = this.emptyForm();
        this.dialogVisible = true;
    }

    openEdit(p: Plan) {
        this.editing.set(p);
        this.form = { name: p.name, description: p.description ?? '', price: p.price, durationValue: p.durationValue, durationUnit: p.durationUnit };
        this.dialogVisible = true;
    }

    applyPreset(preset: (typeof PRESETS)[number]) {
        this.form.durationValue = preset.value;
        this.form.durationUnit = preset.unit;
        if (!this.form.name.trim()) {
            this.form.name = preset.label;
        }
    }

    save(f: NgForm) {
        if (this.saving() || !validateForm(f, this.toast)) return;
        const body: PlanRequest = {
            name: this.form.name.trim(),
            description: this.form.description.trim() || null,
            price: this.form.price ?? 0,
            durationValue: this.form.durationValue ?? 1,
            durationUnit: this.form.durationUnit
        };
        const editing = this.editing();
        this.saving.set(true);
        (editing ? this.api.updatePlan(editing.id, body) : this.api.createPlan(body)).subscribe({
            next: (plan) => {
                this.plans.update((list) => sortPlans(editing ? list.map((x) => (x.id === plan.id ? plan : x)) : [...list, plan]));
                this.toast.add({ severity: 'success', summary: editing ? 'Plan actualizado' : 'Plan creado', detail: plan.name });
                this.saving.set(false);
                this.dialogVisible = false;
            },
            error: (err) => {
                this.saving.set(false);
                this.fail(err);
            }
        });
    }

    toggleActive(p: Plan) {
        const activate = !p.active;
        this.api.setPlanActive(p.id, activate).subscribe({
            next: (plan) => {
                this.plans.update((list) => sortPlans(list.map((x) => (x.id === plan.id ? plan : x))));
                this.toast.add({
                    severity: 'success',
                    summary: activate ? 'Plan activado' : 'Plan desactivado',
                    detail: activate ? `${plan.name} ya se puede vender` : `${plan.name} ya no se puede vender; las membresías vigentes no cambian`
                });
            },
            error: (err) => this.fail(err)
        });
    }

    confirmDelete(p: Plan) {
        this.confirm.confirm({
            header: 'Eliminar plan',
            message: `¿Eliminar "${p.name}"? Si ya tiene ventas no se podrá eliminar; en ese caso desactívalo.`,
            icon: 'pi pi-trash',
            acceptLabel: 'Eliminar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.deletePlan(p.id).subscribe({
                    next: () => {
                        this.plans.update((list) => list.filter((x) => x.id !== p.id));
                        this.toast.add({ severity: 'success', summary: 'Plan eliminado', detail: p.name });
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    private emptyForm(): PlanForm {
        return { name: '', description: '', price: null, durationValue: 1, durationUnit: 'MONTHS' };
    }

    private fail(err: unknown) {
        this.loading.set(false);
        this.toast.add({ severity: 'error', summary: 'Error', detail: apiErrorMessage(err), life: 5000 });
    }
}

function sortPlans(list: Plan[]): Plan[] {
    return [...list].sort((a, b) => Number(b.active) - Number(a.active) || a.price - b.price);
}
