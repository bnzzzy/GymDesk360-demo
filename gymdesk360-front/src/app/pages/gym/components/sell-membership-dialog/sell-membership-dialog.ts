import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, model, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { GymApiService } from '@/app/core/api/gym-api.service';
import { Member, Membership, Plan } from '@/app/core/models/api.models';
import { addDays, fromIsoDate, planEndDate, toIsoDate, today } from '@/app/shared/utils/dates';
import { durationLabel } from '@/app/shared/ui-helpers';

/** Venta (o renovación) de un plan a un socio. */
@Component({
    selector: 'app-sell-membership-dialog',
    standalone: true,
    imports: [FormsModule, CurrencyPipe, DatePipe, ButtonModule, DatePickerModule, DialogModule, InputNumberModule, InputTextModule, MessageModule, SelectModule],
    templateUrl: './sell-membership-dialog.html',
    styleUrl: './sell-membership-dialog.scss'
})
export class SellMembershipDialog {
    private readonly api = inject(GymApiService);
    private readonly toast = inject(MessageService);

    readonly visible = model(false);
    readonly member = input.required<Member>();
    /** Todos los planes; aquí solo se ofrecen los activos. */
    readonly plans = input<Plan[]>([]);
    readonly sold = output<Membership>();

    readonly activePlans = computed(() => this.plans().filter((p) => p.active));
    readonly saving = signal(false);
    readonly error = signal<string | null>(null);

    readonly planId = signal<number | null>(null);
    readonly startDate = signal<Date | null>(null);
    readonly price = signal<number | null>(null);
    notes = '';

    readonly minStart = addDays(today(), -31);

    constructor() {
        // Preparar el formulario en cuanto se pide abrir el diálogo (no en (onShow), que llega
        // al final de la animación y borraría lo que el usuario alcanzó a escribir)
        effect(() => {
            if (this.visible()) untracked(() => this.init());
        });
    }

    readonly selectedPlan = computed(() => this.activePlans().find((p) => p.id === this.planId()) ?? null);

    /** Si tiene días pagados (vigentes o futuros), la renovación empieza el día siguiente al último. Igual que el backend. */
    readonly suggestedStart = computed(() => {
        const paidUntil = fromIsoDate(this.member().paidUntil);
        return paidUntil ? addDays(paidUntil, 1) : today();
    });

    readonly isRenewal = computed(() => !this.startDate() && this.suggestedStart() > today());

    readonly effectiveStart = computed(() => this.startDate() ?? this.suggestedStart());

    readonly endDate = computed(() => {
        const p = this.selectedPlan();
        return p ? planEndDate(this.effectiveStart(), p.durationValue, p.durationUnit) : null;
    });

    readonly discount = computed(() => {
        const p = this.selectedPlan();
        const price = this.price();
        return p && price !== null && price < p.price ? p.price - price : 0;
    });

    private init() {
        this.error.set(null);
        this.startDate.set(null);
        this.notes = '';
        const first = this.activePlans()[0] ?? null;
        this.selectPlan(first?.id ?? null);
    }

    selectPlan(id: number | null) {
        this.planId.set(id);
        this.price.set(this.selectedPlan()?.price ?? null);
    }

    planLabel(p: Plan) {
        return durationLabel(p.durationValue, p.durationUnit);
    }

    save() {
        const plan = this.selectedPlan();
        if (this.saving()) return;
        if (!plan || this.price() === null) {
            this.toast.add({ severity: 'warn', summary: 'Revisa el formulario', detail: !plan ? 'Selecciona un plan' : 'Ingresa el precio cobrado' });
            return;
        }
        this.saving.set(true);
        this.error.set(null);
        this.api
            .sellMembership(this.member().id, {
                planId: plan.id,
                startDate: toIsoDate(this.startDate()),
                pricePaid: this.price(),
                notes: this.notes.trim() || null
            })
            .subscribe({
                next: (ms) => {
                    this.saving.set(false);
                    this.visible.set(false);
                    this.toast.add({ severity: 'success', summary: 'Membresía registrada', detail: `${ms.planName} para ${this.member().fullName}` });
                    this.sold.emit(ms);
                },
                error: (err) => {
                    this.saving.set(false);
                    this.error.set(apiErrorMessage(err, 'No se pudo registrar la membresía'));
                }
            });
    }
}
