import { Component, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { MenuModule } from 'primeng/menu';
import { AuthService } from '@/app/core/auth/auth.service';
import { DemoService } from '@/app/core/demo/demo.service';
import { ROLE_LABELS } from '@/app/core/models/api.models';
import { LayoutService } from '@/app/layout/service/layout.service';
import { AppLogo } from '@/app/shared/components/logo/logo';
import { ChangePasswordDialog } from '@/app/shared/components/change-password-dialog/change-password-dialog';

@Component({
    selector: 'app-topbar',
    standalone: true,
    imports: [RouterModule, MenuModule, AvatarModule, AppLogo, ChangePasswordDialog],
    templateUrl: './app.topbar.html',
    styleUrl: './app.topbar.scss'
})
export class AppTopbar {
    readonly layoutService = inject(LayoutService);
    readonly auth = inject(AuthService);
    readonly passwordDialog = signal(false);
    private readonly demo = inject(DemoService);

    readonly initials = computed(() =>
        (this.auth.user()?.fullName ?? '?')
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 2)
            .map((w) => w[0].toUpperCase())
            .join('')
    );

    readonly roleLabel = computed(() => {
        const u = this.auth.user();
        if (!u) return '';
        return u.gymName ? `${ROLE_LABELS[u.role]} · ${u.gymName}` : ROLE_LABELS[u.role];
    });

    readonly menuItems = computed<MenuItem[]>(() => [
        { label: this.auth.user()?.email ?? '', disabled: true, icon: 'pi pi-envelope' },
        { separator: true },
        ...(this.demo.enabled() ? [] : [{ label: 'Cambiar contraseña', icon: 'pi pi-key', command: () => this.passwordDialog.set(true) }]),
        { label: 'Cerrar sesión', icon: 'pi pi-sign-out', command: () => this.auth.logout() }
    ]);

    toggleDarkMode() {
        this.layoutService.layoutConfig.update((state) => ({ ...state, darkTheme: !state.darkTheme }));
    }
}
