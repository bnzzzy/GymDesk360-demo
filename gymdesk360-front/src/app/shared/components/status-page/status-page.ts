import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AppLogo } from '@/app/shared/components/logo/logo';

/** Pantalla genérica para 404 / acceso denegado. */
@Component({
    selector: 'app-status-page',
    standalone: true,
    imports: [RouterModule, ButtonModule, AppLogo],
    templateUrl: './status-page.html',
    styleUrl: './status-page.scss'
})
export class StatusPage {
    code = input.required<string>();
    title = input.required<string>();
    message = input.required<string>();
}
