import { Routes } from '@angular/router';
import { AppLayout } from './app/layout/components/layout/app.layout';
import { authGuard, guestGuard, homeRedirectGuard, roleGuard } from './app/core/auth/auth.guards';

export const appRoutes: Routes = [
    {
        path: '',
        component: AppLayout,
        canActivate: [authGuard],
        children: [
            { path: '', pathMatch: 'full', canActivate: [homeRedirectGuard], children: [] },
            {
                path: 'admin',
                canActivate: [roleGuard('SUPER_ADMIN')],
                children: [
                    { path: '', title: 'Panel · GymDesk360', loadComponent: () => import('./app/pages/admin/dashboard/admin-dashboard').then((m) => m.AdminDashboard) },
                    { path: 'gimnasios', title: 'Gimnasios · GymDesk360', loadComponent: () => import('./app/pages/admin/gyms/gyms').then((m) => m.GymsPage) },
                    { path: 'usuarios', title: 'Usuarios · GymDesk360', loadComponent: () => import('./app/pages/admin/users/users').then((m) => m.UsersPage) }
                ]
            },
            {
                path: '',
                canActivate: [roleGuard('ADMIN', 'STAFF')],
                children: [
                    { path: 'panel', title: 'Panel · GymDesk360', loadComponent: () => import('./app/pages/gym/dashboard/gym-dashboard').then((m) => m.GymDashboardPage) },
                    { path: 'socios', title: 'Socios · GymDesk360', loadComponent: () => import('./app/pages/gym/members/members').then((m) => m.MembersPage) },
                    { path: 'socios/:id', title: 'Socio · GymDesk360', loadComponent: () => import('./app/pages/gym/member-detail/member-detail').then((m) => m.MemberDetailPage) },
                    { path: 'planes', title: 'Planes · GymDesk360', loadComponent: () => import('./app/pages/gym/plans/plans').then((m) => m.PlansPage) }
                ]
            },
            { path: 'inicio', title: 'Inicio · GymDesk360', loadComponent: () => import('./app/pages/home/home').then((m) => m.HomePage) }
        ]
    },
    { path: 'auth/login', title: 'Iniciar sesión · GymDesk360', canActivate: [guestGuard], loadComponent: () => import('./app/pages/auth/login/login').then((m) => m.LoginPage) },
    { path: 'auth/access', title: 'Acceso denegado · GymDesk360', loadComponent: () => import('./app/pages/auth/access/access').then((m) => m.AccessPage) },
    { path: 'notfound', title: 'No encontrado · GymDesk360', loadComponent: () => import('./app/pages/notfound/notfound').then((m) => m.NotFoundPage) },
    { path: '**', redirectTo: '/notfound' }
];
