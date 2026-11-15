import { Component, inject, input, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { apiErrorMessage } from '@/app/core/api/admin-api.service';
import { AuthService } from '@/app/core/auth/auth.service';
import { DemoAccount, DemoService } from '@/app/core/demo/demo.service';
import { AppLogo } from '@/app/shared/components/logo/logo';
import { FieldError } from '@/app/shared/components/field-error/field-error';
import { validateForm } from '@/app/shared/utils/forms';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [FormsModule, ButtonModule, InputTextModule, PasswordModule, MessageModule, AppLogo, FieldError],
    templateUrl: './login.html',
    styleUrl: './login.scss'
})
export class LoginPage {
    private readonly auth = inject(AuthService);
    private readonly router = inject(Router);
    private readonly toast = inject(MessageService);
    readonly demo = inject(DemoService);

    readonly demoAccounts: { account: DemoAccount; label: string; hint: string; icon: string }[] = [
        { account: 'admin', label: 'Administrador del gimnasio', hint: 'Socios, planes, ventas y métricas', icon: 'pi pi-briefcase' },
        { account: 'staff', label: 'Recepción (staff)', hint: 'Atiende socios y vende planes', icon: 'pi pi-id-card' },
        { account: 'super-admin', label: 'Dueño de la plataforma', hint: 'Gimnasios clientes y usuarios', icon: 'pi pi-crown' }
    ];

    /** ?redirect=/ruta (lo agrega el authGuard). */
    readonly redirect = input<string>();

    email = '';
    password = '';
    readonly loading = signal(false);
    readonly error = signal<string | null>(null);
    readonly year = new Date().getFullYear();

    readonly features = [
        { icon: 'pi pi-users', text: 'Gestión de socios y membresías' },
        { icon: 'pi pi-qrcode', text: 'Control de acceso con código QR' },
        { icon: 'pi pi-chart-line', text: 'Métricas de tu negocio en tiempo real' }
    ];

    demoLogin(account: DemoAccount) {
        if (this.loading()) return;
        this.loading.set(true);
        this.error.set(null);
        this.auth.demoLogin(account).subscribe({
            next: (user) => this.router.navigateByUrl(this.auth.homeFor(user.role)),
            error: (err) => {
                this.error.set(apiErrorMessage(err, 'No se pudo entrar a la demo'));
                this.loading.set(false);
            }
        });
    }

    submit(f: NgForm) {
        if (this.loading() || !validateForm(f, this.toast)) return;
        this.loading.set(true);
        this.error.set(null);

        this.auth.login(this.email.trim(), this.password).subscribe({
            next: (user) => {
                const target = this.redirect() && this.redirect() !== '/' ? this.redirect()! : this.auth.homeFor(user.role);
                this.router.navigateByUrl(target);
            },
            error: (err) => {
                this.error.set(apiErrorMessage(err, 'No se pudo iniciar sesión'));
                this.loading.set(false);
            }
        });
    }
}
