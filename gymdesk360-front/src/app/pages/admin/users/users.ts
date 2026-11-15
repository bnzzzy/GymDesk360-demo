import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AdminApiService, apiErrorMessage } from '@/app/core/api/admin-api.service';
import { AuthService } from '@/app/core/auth/auth.service';
import { Gym, Role, ROLE_LABELS, User } from '@/app/core/models/api.models';
import { FieldError } from '@/app/shared/components/field-error/field-error';
import { ROLE_OPTIONS, ROLE_SEVERITY } from '@/app/shared/ui-helpers';
import { validateForm } from '@/app/shared/utils/forms';

interface UserForm {
    fullName: string;
    email: string;
    password: string;
    role: Role;
    gymId: number | null;
}

@Component({
    selector: 'app-users',
    standalone: true,
    imports: [FormsModule, RouterModule, MessageModule, FieldError, AvatarModule, ButtonModule, DialogModule, IconFieldModule, InputIconModule, InputTextModule, PasswordModule, SelectModule, TableModule, TagModule, TooltipModule],
    templateUrl: './users.html',
    styleUrl: './users.scss'
})
export class UsersPage implements OnInit {
    private readonly api = inject(AdminApiService);
    private readonly auth = inject(AuthService);
    private readonly toast = inject(MessageService);
    private readonly confirm = inject(ConfirmationService);
    private readonly router = inject(Router);

    /** ?gymId=3 filtra por gimnasio (atajo desde la tabla de gimnasios). */
    readonly gymId = input<string>();

    readonly users = signal<User[]>([]);
    readonly gyms = signal<Gym[]>([]);
    readonly gymFilter = signal<number | null>(null);
    readonly loading = signal(true);
    readonly saving = signal(false);
    readonly editing = signal<User | null>(null);
    readonly passwordUser = signal<User | null>(null);

    readonly me = computed(() => this.auth.user()?.id);
    readonly editingSelf = computed(() => this.editing()?.id === this.me());
    readonly roleOptions = ROLE_OPTIONS;

    dialogVisible = false;
    passwordVisible = false;
    newPassword = '';
    form: UserForm = this.emptyForm();

    ngOnInit() {
        const initial = Number(this.gymId());
        this.gymFilter.set(Number.isFinite(initial) && initial > 0 ? initial : null);
        this.api.listGyms().subscribe({ next: (g) => this.gyms.set(g), error: (err) => this.fail(err) });
        this.loadUsers();
    }

    loadUsers() {
        this.loading.set(true);
        this.api.listUsers(this.gymFilter()).subscribe({
            next: (users) => {
                this.users.set(users);
                this.loading.set(false);
            },
            error: (err) => this.fail(err)
        });
    }

    onGymFilter(gymId: number | null) {
        this.gymFilter.set(gymId ?? null);
        this.router.navigate([], { queryParams: { gymId: gymId ?? null }, replaceUrl: true });
        this.loadUsers();
    }

    openNew() {
        this.editing.set(null);
        this.form = this.emptyForm();
        this.dialogVisible = true;
    }

    openEdit(u: User) {
        this.editing.set(u);
        this.form = { fullName: u.fullName, email: u.email, password: '', role: u.role, gymId: u.gymId };
        this.dialogVisible = true;
    }

    save(f: NgForm) {
        if (this.saving() || !validateForm(f, this.toast)) return;
        const editing = this.editing();
        const body = {
            fullName: this.form.fullName,
            email: this.form.email.trim(),
            role: this.form.role,
            gymId: this.form.role === 'SUPER_ADMIN' ? null : this.form.gymId,
            ...(editing ? {} : { password: this.form.password })
        };
        this.saving.set(true);
        const req$ = editing ? this.api.updateUser(editing.id, body) : this.api.createUser(body);
        req$.subscribe({
            next: (user) => {
                const visible = !this.gymFilter() || user.gymId === this.gymFilter();
                this.users.update((list) => {
                    const rest = list.filter((x) => x.id !== user.id);
                    return visible ? (editing ? list.map((x) => (x.id === user.id ? user : x)) : [...list, user]) : rest;
                });
                this.toast.add({ severity: 'success', summary: editing ? 'Usuario actualizado' : 'Usuario creado', detail: user.fullName });
                this.saving.set(false);
                this.dialogVisible = false;
            },
            error: (err) => {
                this.saving.set(false);
                this.fail(err);
            }
        });
    }

    openPassword(u: User) {
        this.passwordUser.set(u);
        this.newPassword = '';
        this.passwordVisible = true;
    }

    savePassword(f: NgForm) {
        const u = this.passwordUser();
        if (!u || this.saving() || !validateForm(f, this.toast)) return;
        this.saving.set(true);
        this.api.resetPassword(u.id, this.newPassword).subscribe({
            next: () => {
                this.toast.add({ severity: 'success', summary: 'Contraseña restablecida', detail: u.fullName });
                this.saving.set(false);
                this.passwordVisible = false;
            },
            error: (err) => {
                this.saving.set(false);
                this.fail(err);
            }
        });
    }

    toggleEnabled(u: User) {
        const enable = !u.enabled;
        this.confirm.confirm({
            header: enable ? 'Activar usuario' : 'Desactivar usuario',
            message: enable ? `¿Reactivar a ${u.fullName}?` : `¿Desactivar a ${u.fullName}? Perderá el acceso de inmediato.`,
            icon: enable ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
            acceptLabel: enable ? 'Activar' : 'Desactivar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: enable ? 'success' : 'warn' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.setUserEnabled(u.id, enable).subscribe({
                    next: (user) => {
                        this.users.update((list) => list.map((x) => (x.id === user.id ? user : x)));
                        this.toast.add({ severity: 'success', summary: enable ? 'Usuario activado' : 'Usuario desactivado', detail: user.fullName });
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    confirmDelete(u: User) {
        this.confirm.confirm({
            header: 'Eliminar usuario',
            message: `Se eliminará la cuenta de ${u.fullName} (${u.email}). Esta acción no se puede deshacer.`,
            icon: 'pi pi-trash',
            acceptLabel: 'Eliminar',
            rejectLabel: 'Cancelar',
            acceptButtonProps: { severity: 'danger' },
            rejectButtonProps: { severity: 'secondary', text: true },
            accept: () =>
                this.api.deleteUser(u.id).subscribe({
                    next: () => {
                        this.users.update((list) => list.filter((x) => x.id !== u.id));
                        this.toast.add({ severity: 'success', summary: 'Usuario eliminado', detail: u.fullName });
                    },
                    error: (err) => this.fail(err)
                })
        });
    }

    initials(name: string): string {
        return name
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 2)
            .map((w) => w[0].toUpperCase())
            .join('');
    }

    roleLabel(r: Role) {
        return ROLE_LABELS[r];
    }

    roleSeverity(r: Role) {
        return ROLE_SEVERITY[r];
    }

    private emptyForm(): UserForm {
        return { fullName: '', email: '', password: '', role: 'MEMBER', gymId: this.gymFilter?.() ?? null };
    }

    private fail(err: unknown) {
        this.loading.set(false);
        this.toast.add({ severity: 'error', summary: 'Error', detail: apiErrorMessage(err), life: 5000 });
    }
}
