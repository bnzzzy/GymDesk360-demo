import { Component, input } from '@angular/core';
import { NgForm, NgModel } from '@angular/forms';

/**
 * Mensaje de error bajo un campo de formulario (template-driven).
 * Aparece cuando el campo es inválido y el usuario ya lo tocó o intentó enviar el formulario.
 *
 *   <input pInputText name="email" [(ngModel)]="email" #emailCtrl="ngModel" required email />
 *   <app-field-error [control]="emailCtrl" [form]="f" />
 */
@Component({
    selector: 'app-field-error',
    standalone: true,
    templateUrl: './field-error.html',
    styleUrl: './field-error.scss'
})
export class FieldError {
    readonly control = input.required<NgModel>();
    readonly form = input<NgForm>();
    /** Texto para errores de "pattern" (formato). */
    readonly patternMessage = input('Formato inválido');
    /** Error adicional que no viene del control (p. ej. RUT inválido, contraseñas distintas). */
    readonly customError = input<string | null>(null);

    get message(): string | null {
        const c = this.control();
        const visible = c.touched || c.dirty || !!this.form()?.submitted;
        if (!visible) return null;
        if (this.customError()) return this.customError();

        const e = c.errors;
        if (!e) return null;
        if (e['required']) return 'Este campo es obligatorio';
        if (e['email']) return 'Ingresa un email válido';
        if (e['minlength']) return `Debe tener al menos ${e['minlength'].requiredLength} caracteres`;
        if (e['maxlength']) return `Máximo ${e['maxlength'].requiredLength} caracteres`;
        if (e['pattern']) return this.patternMessage();
        if (e['min']) return `El valor mínimo es ${e['min'].min}`;
        if (e['max']) return `El valor máximo es ${e['max'].max}`;
        return 'Valor inválido';
    }
}
