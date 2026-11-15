import { Component, computed, inject } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { AuthService } from '@/app/core/auth/auth.service';
import { AppMenuitem } from '@/app/layout/components/menuitem/app.menuitem';

@Component({
    selector: 'app-menu',
    standalone: true,
    imports: [AppMenuitem],
    templateUrl: './app.menu.html',
    styleUrl: './app.menu.scss'
})
export class AppMenu {
    private readonly auth = inject(AuthService);

    readonly model = computed<MenuItem[]>(() => {
        switch (this.auth.user()?.role) {
            case 'SUPER_ADMIN':
                return SUPER_ADMIN_MENU;
            case 'ADMIN':
            case 'STAFF':
                return GYM_MENU;
            default:
                return MEMBER_MENU;
        }
    });
}

const SUPER_ADMIN_MENU: MenuItem[] = [
    {
        label: 'Plataforma',
        items: [
            { label: 'Panel', icon: 'pi pi-fw pi-chart-bar', routerLink: ['/admin'] },
            { label: 'Gimnasios', icon: 'pi pi-fw pi-building', routerLink: ['/admin/gimnasios'] },
            { label: 'Usuarios', icon: 'pi pi-fw pi-users', routerLink: ['/admin/usuarios'] }
        ]
    }
];

const GYM_MENU: MenuItem[] = [
    {
        label: 'Mi gimnasio',
        items: [
            { label: 'Panel', icon: 'pi pi-fw pi-home', routerLink: ['/panel'] },
            { label: 'Socios', icon: 'pi pi-fw pi-id-card', routerLink: ['/socios'], routerLinkActiveOptions: { paths: 'subset', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' } },
            { label: 'Planes', icon: 'pi pi-fw pi-tags', routerLink: ['/planes'] }
        ]
    }
];

const MEMBER_MENU: MenuItem[] = [
    {
        label: 'Mi cuenta',
        items: [{ label: 'Inicio', icon: 'pi pi-fw pi-home', routerLink: ['/inicio'] }]
    }
];
