import { Component, effect, inject, model, signal, untracked } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { AuthService } from '@/app/core/auth/auth.service';
import { FieldError } from '@/app/shared/components/field-error/field-error';
import { validateForm } from '@/app/shared/utils/forms';

@Component({
    selector: 'app-change-password-dialog',
    standalone: true,
    imports: [FormsModule, ButtonModule, DialogModule, MessageModule, PasswordModule, FieldError],
    templateUrl: './change-password-dialog.html',
    styleUrl: './change-password-dialog.scss'
})
export class ChangePasswordDialog {
    private readonly auth = inject(AuthService);
    private readonly toast = inject(MessageService);

    readonly visible = model(false);
    readonly saving = signal(false);
    readonly error = signal<string | null>(null);

    current = '';
    next = '';
    confirm = '';

    constructor() {
        // Preparar el formulario en cuanto se pide abrir el diálogo (no en (onShow), que llega
        // al final de la animación y borraría lo que el usuario alcanzó a escribir)
        effect(() => {
            if (this.visible()) untracked(() => this.reset());
        });
    }

    private reset() {
        this.current = this.next = this.confirm = '';
        this.error.set(null);
    }

    save(f: NgForm) {
        const mismatch = this.confirm && this.next !== this.confirm ? 'Las contraseñas nuevas no coinciden' : null;
        if (this.saving() || !validateForm(f, this.toast, mismatch)) return;
        this.saving.set(true);
        this.error.set(null);
        this.auth.changePassword(this.current, this.next).subscribe({
            next: () => {
                this.saving.set(false);
                this.visible.set(false);
                this.toast.add({ severity: 'success', summary: 'Contraseña actualizada', detail: 'Tus otras sesiones fueron cerradas.' });
            },
            error: (err) => {
                this.saving.set(false);
                this.error.set(apiErrorMessage(err, 'No se pudo cambiar la contraseña'));
            }
        });
    }
}
