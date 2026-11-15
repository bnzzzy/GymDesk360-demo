import { Component, input } from '@angular/core';

let nextId = 0;

/**
 * Isotipo de GymDesk360: anillo de 360° con flecha de giro y mancuerna al centro, en oro degradado.
 * Con [wordmark]="true" agrega el texto "GymDesk360".
 */
@Component({
    selector: 'app-logo',
    standalone: true,
    templateUrl: './logo.html',
    styleUrl: './logo.scss'
})
export class AppLogo {
    size = input(40);
    wordmark = input(false);
    /** Fondo negro redondeado (como el favicon). */
    badge = input(false);
    /** Texto blanco siempre (para fondos oscuros en cualquier tema). */
    inverse = input(false);

    readonly gid = `gd-gold-${nextId++}`;
    readonly fill = `url(#${this.gid})`;
}
