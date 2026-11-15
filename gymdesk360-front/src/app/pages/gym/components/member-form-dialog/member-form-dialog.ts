import { Component, effect, inject, input, model, output, signal, untracked } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { GymApiService } from '@/app/core/api/gym-api.service';
import { Member, MemberRequest } from '@/app/core/models/api.models';
import { FieldError } from '@/app/shared/components/field-error/field-error';
import { fromIsoDate, toIsoDate, today } from '@/app/shared/utils/dates';
import { validateForm } from '@/app/shared/utils/forms';
import { formatRut, isValidRut } from '@/app/shared/utils/rut';

interface MemberForm {
    rut: string;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    birthDate: Date | null;
    emergencyContactName: string;
    emergencyContactPhone: string;
    notes: string;
}

/** Alta / edición de un socio. Con [member] = null crea uno nuevo. */
@Component({
    selector: 'app-member-form-dialog',
    standalone: true,
    imports: [FormsModule, ButtonModule, DatePickerModule, DialogModule, InputTextModule, MessageModule, TextareaModule, FieldError],
    templateUrl: './member-form-dialog.html',
    styleUrl: './member-form-dialog.scss'
})
export class MemberFormDialog {
    private readonly api = inject(GymApiService);
    private readonly toast = inject(MessageService);

    readonly visible = model(false);
    readonly member = input<Member | null>(null);
    readonly saved = output<Member>();

    readonly saving = signal(false);
    readonly error = signal<string | null>(null);
    readonly maxBirthDate = today();

    form: MemberForm = emptyForm();

    constructor() {
        // Preparar el formulario en cuanto se pide abrir el diálogo (no en (onShow), que llega
        // al final de la animación y borraría lo que el usuario alcanzó a escribir)
        effect(() => {
            if (this.visible()) untracked(() => this.init());
        });
    }

    private init() {
        const m = this.member();
        this.error.set(null);
        this.form = m
            ? {
                  rut: formatRut(m.rut),
                  firstName: m.firstName,
                  lastName: m.lastName,
                  email: m.email ?? '',
                  phone: m.phone ?? '',
                  birthDate: fromIsoDate(m.birthDate),
                  emergencyContactName: m.emergencyContactName ?? '',
                  emergencyContactPhone: m.emergencyContactPhone ?? '',
                  notes: m.notes ?? ''
              }
            : emptyForm();
    }

    get rutInvalid(): boolean {
        return !isValidRut(this.form.rut);
    }

    onRutBlur() {
        this.form.rut = formatRut(this.form.rut.trim());
    }

    save(f: NgForm) {
        if (this.saving() || !validateForm(f, this.toast, this.rutInvalid ? 'El RUT ingresado no es válido' : null)) return;
        const body: MemberRequest = {
            rut: this.form.rut.trim() || null,
            firstName: this.form.firstName.trim(),
            lastName: this.form.lastName.trim(),
            email: this.form.email.trim() || null,
            phone: this.form.phone.trim() || null,
            birthDate: toIsoDate(this.form.birthDate),
            emergencyContactName: this.form.emergencyContactName.trim() || null,
            emergencyContactPhone: this.form.emergencyContactPhone.trim() || null,
            notes: this.form.notes.trim() || null
        };
        const editing = this.member();
        this.saving.set(true);
        this.error.set(null);
        (editing ? this.api.updateMember(editing.id, body) : this.api.createMember(body)).subscribe({
            next: (m) => {
                this.saving.set(false);
                this.visible.set(false);
                this.toast.add({ severity: 'success', summary: editing ? 'Socio actualizado' : 'Socio registrado', detail: m.fullName });
                this.saved.emit(m);
            },
            error: (err) => {
                this.saving.set(false);
                this.error.set(apiErrorMessage(err, 'No se pudo guardar el socio'));
            }
        });
    }
}

function emptyForm(): MemberForm {
    return { rut: '', firstName: '', lastName: '', email: '', phone: '', birthDate: null, emergencyContactName: '', emergencyContactPhone: '', notes: '' };
}
