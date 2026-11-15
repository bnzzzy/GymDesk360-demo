import { NgForm } from '@angular/forms';
import { MessageService } from 'primeng/api';

/**
 * Antes de enviar: si el formulario es inválido marca todos los campos (para que muestren su error),
 * avisa con un toast y devuelve false.
 *
 * @param extraError error que no detecta Angular (p. ej. RUT inválido); si viene, también bloquea el envío
 */
export function validateForm(form: NgForm, toast: MessageService, extraError?: string | null): boolean {
    if (form.valid && !extraError) return true;
    for (const control of Object.values(form.controls)) {
        control.markAsTouched();
        control.markAsDirty();
    }
    toast.add({
        severity: 'warn',
        summary: 'Revisa el formulario',
        detail: extraError ?? 'Hay campos sin completar o con errores. Están marcados en rojo.',
        life: 4000
    });
    return false;
}
