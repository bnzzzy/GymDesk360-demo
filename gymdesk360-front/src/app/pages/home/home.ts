import { Component, inject } from '@angular/core';
import { AuthService } from '@/app/core/auth/auth.service';
import { ROLE_LABELS } from '@/app/core/models/api.models';

/** Inicio provisorio para ADMIN / STAFF / MEMBER mientras se construyen sus módulos. */
@Component({
    selector: 'app-home',
    standalone: true,
    templateUrl: './home.html',
    styleUrl: './home.scss'
})
export class HomePage {
    readonly auth = inject(AuthService);
    readonly roleLabel = ROLE_LABELS[this.auth.user()?.role ?? 'MEMBER'];
}
